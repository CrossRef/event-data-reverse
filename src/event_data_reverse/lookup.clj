(ns event-data-reverse.lookup
  (:require [event-data-reverse.structured-extraction :as structured-extraction]
            [event-data-reverse.unstructured-extraction :as unstructured-extraction])
  (:require [crossref.util.doi :as crdoi])
  (:require [clojure.string :as string])
  (:require [clojure.tools.logging :refer [info]])
  (:require [net.cgrand.enlive-html :as html]
            [cemerick.url :as cemerick-url]
            [robert.bruce :refer [try-try-again]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json])
  (:import [java.net URL URI URLEncoder URLDecoder]))

(def whole-doi-re #"^10\.\d{4,9}/[^\s]+$")
(def doi-re #"(10\.\d{4,9}/[^\s]+)")
(def doi-encoded-re #"10\.\d{4,9}%2[fF][^\s]+")

; TODO not entirely sure what the grammar of ShortDOI is but this seems to fit.
; Unfortunately this also matches the first half of a DOI.
; Match the shortcut URL (e.g. "doi.org/aabbe") or the handle (e.g. "10/aabbe").

; Locate a shortDOI in its natural habitat.
(def shortdoi-find-re #"(?:(?:(?:dx.)?doi.org/)|10/)(?:info:doi/|urn:|doi:)?([a-zA-Z0-9]+)")

; The shortDOI itself is just an alphanumeric string, which isn't particularly disinctive.
(def shortdoi-re #"[a-zA-Z0-9]+")

; https://en.wikipedia.org/wiki/Publisher_Item_Identifier
; Used by Elsevier and others.
(def pii-re #"[SB][0-9XB]{16}")

; Helpers

(defn try-url
  "Try to construct a URL."
  [text]
  (try (new URL text) (catch Exception _ nil)))

(defn try-hostname
  "Try to get a hostname from a URL string."
  [text]
  (try (.getHost (new URL text)) (catch Exception e nil)))

(defn doi-from-url
  "If a URL is a DOI, return the non-URL version of the DOI."
  [text]
  (when-let [url (try-url text)]
    (when (#{"doi.org" "dx.doi.org"} (.getHost url))
      (.substring (or (.getPath url) "") 1))))

(defn matches-doi?
  "Does this look like a DOI?"
  [input]
  (and (not (string/blank? input)) (re-matches whole-doi-re input)))

(defn remove-doi-colon-prefix
  "Turn 'doi:10.5555/12346789' into '10.5555/12345678'"
  [input]
  (when-let [match (re-matches #"^[a-zA-Z ]+: ?(10\.\d+/.*)$" input)]
    (.toLowerCase (second match))))

(defn resolve-doi
  "Resolve a DOI or ShortDOI, expressed as not-URL form. May or may not be URLEscaped. Return the DOI."
  [doi]
  (let [response @(try-try-again {:sleep 500 :tries 2}
                    #(http/get
                      (str "http://doi.org/" doi)
                      {:follow-redirects false}))
        status (:status response)
        redirect-header (-> response :headers :location)]
      (cond
        (:error response) nil

        ; If it's a shortDOI it will redirect to the real one. Use this.
        (= (try-hostname redirect-header) "doi.org") (crdoi/non-url-doi redirect-header)
        ; If it's a real DOI it will return a 30x. 
        (= (quot status 100) 3) (crdoi/non-url-doi doi)

        ; If it's not anything then don't return anything.
        :default nil)))

(defn resolve-doi-maybe-escaped
  "Try to `resolve-doi`, escaped and unescaped."
  [doi]
  (if-let [unescaped (resolve-doi doi)]
    unescaped
    (when-let [escaped (resolve-doi (URLEncoder/encode doi "UTF-8"))]
      (URLDecoder/decode escaped "UTF-8"))))

(def max-drops 5)
(defn validate-doi
  "For a given suspected DOI or shortDOI, validate that it exists against the API, possibly modifying it to get there."
  [doi]
  (loop [i 0
         doi doi]
    ; Terminate if we're at the end of clipping things off or the DOI no longer looks like an DOI. 
    ; The API will return 200 for e.g. "10.", so don't try and feed it things like that.
    (if (or (= i max-drops)
            (nil? doi)
            (< (.length doi) i)
            ; The shortDOI regular expression is rather liberal, but it is what it is.
            (not (or (re-matches doi-re doi) (re-matches shortdoi-re doi))))
      ; Stop recursion.
      nil
      ; Or try this substring.
      (if-let [clean-doi (resolve-doi-maybe-escaped doi)] 
        ; resolve-doi may alter the DOI it returns, e.g. resolving a shortDOI to a real DOI or lower-casing.
        
        ; We have a working DOI!
        ; Just check it does't contain a sneaky question mark which would still resolve e.g. http://www.tandfonline.com/doi/full/10.1080/00325481.2016.1186487?platform=hootsuite
        ; If there is a question mark, try removing it to see if it still works.
        (if (.contains clean-doi "?")
          (let [before-q (first (.split clean-doi "\\?"))]
            (if (resolve-doi before-q)
              before-q
              clean-doi))
          clean-doi)
        
        (recur (inc i) (.substring doi 0 (- (.length doi) 1)))))))

(defn first-valid
  "Return the first valid, possibly cleaned, DOI"
  [dois]
  ; using `keep` on a chunked seq would waste time evaluating whole chunk rather than each in sequence.
  (loop [[doi & tail] dois]
    (if-let [validated (validate-doi doi)]
      validated
      (when (not-empty tail)
        (recur tail)))))

(defn validate-pii
  "Validate a PII and return the DOI if it's been used as an alternative ID."
  [pii]
  (let [result (try-try-again {:sleep 500 :tries 2} #(http/get "http://api.crossref.org/v1/works" {:query-params {:filter (str "alternative-id:" pii)}}))
        body (-> @result :body json/read-str)
        items (get-in body ["message" "items"])]
    ; Only return when there's exactly one match.
    (when (= 1 (count items))
      (get (first items) "DOI"))))

(defn strip-extras-from-url
  "Remove the query string and fragment from a URL"
  [url]
  (new URL (.getProtocol url)
           (.getHost url)
           (.getPort url)
           (.getPath url)))

(defn url-in-set?
  "Fairly liberal test for if needle is in set of URLs.
  Disregards query string and fragment: only useful in context of checking something we suspect to be true."
  [needle-url haystack-urls]
  (let [base-needle (strip-extras-from-url needle-url)]
  (loop [urls haystack-urls]
    (cond
      ; Are they the same file? Removes fragment.
      (.sameFile needle-url (first urls)) true

      ; Try removing the query string.
      (= base-needle (strip-extras-from-url (first urls))) true

      (not-empty (rest urls)) (recur (rest urls))
      :default nil))))

(defn url-matches-doi?
  "Does the given DOI resolve to the given URL? Return DOI if so."
  [url doi]
  (info "Check " url " for " doi)
  (when-let [real-url (try-url url)]
    (when-let [; URL may have a query string on the end. So construct some candidates.
               result (try-try-again {:sleep 500 :tries 2} #(http/get (str "http://doi.org/" doi)
                                                           {:follow-redirects true
                                                            :throw-exceptions true
                                                            :socket-timeout 5000
                                                            :conn-timeout 5000
                                                            :headers {"Referer" "chronograph.crossref.org"
                                                                      "User-Agent" "CrossRefDOICheckerBot (labs@crossref.org)"}}))]
      (let [doi-urls (set (conj (-> @result :trace-redirects) (-> @result :opts :url)))
            doi-real-urls (keep try-url doi-urls)

            ; Now we have a java.net.URL that we're concerned with and a set of java.net.URLs that we're trying to match.
            url-match (url-in-set? real-url doi-real-urls)]
        (when url-match
          doi)))))

(defn extract-text-fragments-from-html
  "Extract all text from an HTML document."
  [input]
  (string/join " "
    (-> input
    (html/html-snippet)
    (html/select [:body html/text-node])
    (html/transform [:script] nil)
    (html/texts))))

; DOI Extraction
; Extract things that look like DOIs. Don't validate them yet.

(defn extract-doi-from-get-params
  "If there's a DOI in a get parameter of a URL, find it"
  [url]
  (try
    (let [params (-> url cemerick-url/query->map clojure.walk/keywordize-keys)
          doi-like (keep (fn [[k v]] (when (re-matches whole-doi-re v) v)) params)]
      (first doi-like))
    ; Some things look like URLs but turn out not to be.
    (catch IllegalArgumentException _ nil)))

(defn extract-doi-in-a-hrefs-from-html
  "Extract all <a href> links from an HTML document. DEPRECATED"
  [input]
    (let [links (html/select (html/html-snippet input) [:a])
          hrefs (keep #(-> % :attrs :href) links)
          dois (keep doi-from-url hrefs)]
      (distinct dois)))

(defn extract-potential-dois-from-text
  "Extract potential DOIs from arbitrary text, including URL-encoded ones which will be unencoded."
  [text]
  ; doi-re and short-doi-find-re have a capture group for the actual value we want to find, hence `second`.
  (let [matches (map second (concat (re-seq doi-re text) (re-seq shortdoi-find-re text)))
        encoded-matches (map #(URLDecoder/decode %) (re-seq doi-encoded-re text))]
        (distinct (concat encoded-matches matches))))

(defn extract-potential-piis-from-text
  [text]
  (let [matches (re-seq pii-re text)]
        (distinct matches)))

(defn fetch
  "Fetch the content at a URL, following redirects and accepting cookies."
  [url]
  (loop [headers {"Referer" "eventdata.crossref.org"
                  "User-Agent" "CrossrefEventDataBot (labs@crossref.org)"}
         depth 0
         url url]
    (if (> depth 4)
      nil
      (let [result @(org.httpkit.client/get url {:follow-redirects false :headers headers :as :text})
            cookie (-> result :headers :set-cookie)
            new-headers (merge headers (when cookie {"Cookie" cookie}))]

        (condp = (:status result)
          200 result
          ; Weirdly some Nature pages return 401 with the content. http://www.nature.com/nrendo/journal/v10/n9/full/nrendo.2014.114.html
          401 result
          302 (recur new-headers (inc depth) (-> result :headers :location))
          nil)))))

(def recognised-content-types
  "Content types we'll allow ourselves to inspect.
  Notable in its absence is PDF, for now."
   #{"text/plain" "text/html"})

(defn resolve-doi-from-url
  "Take a URL and try to resolve it to find what valid DOI it corresponds to."
  [url]
  (info "Attempt resolve-doi-from-url: " url)
  ; Check if we want to bother with this URL.
    (when-let [result (try-try-again {:sleep 500 :tries 2} #(fetch url))]
      (when (recognised-content-types
              (.getBaseType (new javax.mail.internet.ContentType (.toLowerCase (get-in result [:headers :content-type] "unknown/unknown")))))
        (let [body (:body result)

              doi-from-structured (structured-extraction/from-tags body)
              doi-from-unstructured (unstructured-extraction/from-webpage body url)

              ; DOI candidates in order of likelihood
              candidates (distinct [doi-from-structured doi-from-unstructured])

              ; Validate ones that exist. The regular expression might be a bit greedy, so this may chop bits off the end to make it work.
              valid-doi (first-valid candidates)

              ; NB not using url-maches-doi, maybe reintroduce.
              ]

          (info "Found from structured HTML:", doi-from-structured)
          (info "Found from unstructured text:" doi-from-unstructured)
          (info "Valid DOI: " valid-doi)
          valid-doi))))

; Combined methods.
; Combine extraction methods and validate.

(defn get-embedded-doi-from-string
  "Get valid DOI that's embedded in a URL (or an arbitrary string) by a number of methods."
  [url]
  (info "Attempt get-embedded-doi-from-string")
  ; First see if cleanly represented it's in the GET params.
  (if-let [doi (-> url extract-doi-from-get-params validate-doi)]
    doi
    ; Next try extracting DOIs and/or PII with regular expressions.
    (let [potential-dois (extract-potential-dois-from-text url)
          validated-doi (first-valid potential-dois)
          potential-alternative-ids (extract-potential-piis-from-text url)
          validated-pii-doi (->> potential-alternative-ids (keep validate-pii) first)]

      (if (or validated-doi validated-pii-doi)
        (or validated-doi validated-pii-doi)

        ; We may need to do extra things.
        ; Try splitting in various places.
        (let [; e.g. nomos-elibrary.de
              last-slash (map #(clojure.string/replace % #"^(10\.\d+/(.*))/.*$" "$1") potential-dois)

              ; e.g. ijorcs.org
              first-slash (map #(clojure.string/replace % #"^(10\.\d+/(.*?))/.*$" "$1") potential-dois)

              ; e.g. SICIs
              semicolon (map #(clojure.string/replace % #"^(10\.\d+/(.*));.*$" "$1") potential-dois)
              
              ; eg. JSOR
              hashchar (map #(clojure.string/replace % #"^(10\.\d+/(.*?))#.*$" "$1") potential-dois)

              ; e.g. biomedcentral
              question-mark (map #(clojure.string/replace % #"^(10\.\d+/(.*?))\?.*$" "$1") potential-dois)

              ; e.g. citeseerx
              amp-mark (map #(clojure.string/replace % #"^(10\.\d+/(.*?))&.*$" "$1") potential-dois)

              candidates (distinct (concat first-slash last-slash semicolon hashchar question-mark amp-mark))
              
              ; Lots of these produce duplicates.
              distinct-candidates (distinct candidates)

              ; Now take the first one that we could validate.
              doi (first-valid distinct-candidates)]
          doi))))) 

(defn cleanup-doi
  "Take a URL or DOI or something that could be a DOI, return the valid DOI if it is one."
  [potential-doi]
  (info "Attempt cleanup-doi")
  (when (or 
    (re-matches whole-doi-re potential-doi)
    (re-find doi-re potential-doi)
    (re-find doi-encoded-re potential-doi)
    (re-matches shortdoi-find-re potential-doi))
  (let [normalized-doi (crdoi/non-url-doi potential-doi)
        doi-colon-prefixed-doi (remove-doi-colon-prefix potential-doi)]

    ; Find the first operation that produces an output that looks like a DOI.
    (first-valid [potential-doi normalized-doi doi-colon-prefixed-doi]))))
  
; External functions.

(defn lookup
  "Lookup a DOI from an input. Return only valid DOI."
  [input ]
  ; Try to treat it as a DOI in a particular encoding.
  (if-let [cleaned-valid-doi (cleanup-doi input)]
    [:cleaned cleaned-valid-doi]

    ; Try to treat it as a Publisher URL that has a DOI in the URL, or a string with a DOI in it somehow.
    (if-let [embedded-valid-doi (validate-doi (get-embedded-doi-from-string input))]
      [:embedded embedded-valid-doi]

    ; Try to treat it as a Publisher URL that must be fetched to extract its DOI.
    (if-let [resolved-valid-doi (when-let [url (try-url input)] (resolve-doi-from-url input))]
      [:resolved resolved-valid-doi]
      nil))))

