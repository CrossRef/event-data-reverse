(ns event-data-reverse.unstructured-extraction
  "Extract DOIs from plain text and HTML various ways."
  (:require [crossref.util.doi :as util])
  (:import [org.jsoup Jsoup]))

(def doi-re #"(10\.\d{4,9}/[^\s]+)")

(defn from-text-tags
  "Extract DOIs from the structure of text tags."
  [text]
  (let [interested-values (-> text
                            Jsoup/parse
                            ; All tags
                            (.select "*")
                            ; Text of each.
                            (#(map (fn [tag] (.text tag)) %))
                            ; Search for DOIs in text of each.
                            (#(apply concat (mapcat (fn [element-text] (re-seq doi-re element-text)) %)))
                            (#(filter (fn [potential-doi] (util/well-formed potential-doi)) %))
                            ; As a DOI will be retrieved various times at different levels, make a set.
                            (set))]
    interested-values))


(defn from-webpage
  "Extract the most likely DOI from an HTML page given its URL."
  [html url]
  (let [possible-dois (from-text-tags html)
        ; Filter those whose suffix appears in the URL.
        found-in-url (filter (fn [doi] (.contains url (util/get-suffix doi))) possible-dois)]
    (first found-in-url)))
