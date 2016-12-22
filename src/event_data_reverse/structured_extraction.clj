(ns event-data-reverse.structured-extraction
  "Extract DOIs from structured HTML various ways."
  (:require [crossref.util.doi :as util])
  (:import [org.jsoup Jsoup]))

(def interested-tag-attrs
  "List of selectors whose attrs we're interested in."
  [
    ["meta[name=citation_doi]" "content"] ; e.g. http://www.ncbi.nlm.nih.gov/pmc/articles/PMC4852986/?report=classic
    ["meta[name=DC.Identifier]" "content"]; e.g. http://pubsonline.informs.org/doi/abs/10.1287/mnsc.2016.2427
    ["meta[name=DC.identifier]" "content"]; e.g. https://figshare.com/articles/A_Modeler_s_Tale/3423371/1
    ["meta[name=DC.Identifier.DOI]" "content"] ; e.g. http://www.circumpolarhealthjournal.net/index.php/ijch/article/view/18594/html
    ["meta[name=DC.Source]" "content"]; e.g. http://pubsonline.informs.org/doi/abs/10.1287/mnsc.2016.2427
    ["meta[name=prism.doi]" "content"]; e.g.  http://ccforum.biomedcentral.com/articles/10.1186/s13054-016-1322-5
  ])

(def interested-tag-text
  "List of selectors whose text content we're interested in."
  [
    "span.slug-doi" ; e.g. http://jnci.oxfordjournals.org/content/108/6/djw160.full
  ])

(defn from-tags
  "Extract DOI from Metadata tags."
  [text]
  (let [document (Jsoup/parse text)
        ; Get specific attribute values from named elements.
        interested-attr-values (mapcat (fn [[selector attr-name]]
                                                (->>
                                                  (.select document selector)
                                                  (map #(.attr % attr-name))))
                                              interested-tag-attrs)

        ; Get text values from named elements.
        interested-text-values (mapcat (fn [selector]
                                                (->>
                                                  (.select document selector)
                                                  (map #(.text %))))
                                              interested-tag-text)

        interested-values (->
                            (concat interested-attr-values interested-text-values)
                            
                            ; normalize DOI
                            (#(keep (fn [potential-doi] 
                              (util/non-url-doi potential-doi)) %))
                            first)]
    interested-values))
