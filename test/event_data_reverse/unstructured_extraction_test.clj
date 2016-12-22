(ns event-data-reverse.unstructured-extraction-test
  (:require [clojure.test :refer :all]
            [event-data-reverse.unstructured-extraction :as unstructured-extraction]))


(deftest from-tag-text
  (testing "DOIs in plain text of HTML can be extracted."
    (is (=
          (unstructured-extraction/from-text-tags (slurp "resources/test/html/www.nber.org-papers-w22307.xyz") )
          #{"10.3386/w22307"}))))


(deftest from-webpage
  (testing "Single DOI in plain text of HTML can be extracted from webpage."
    (is (=
          (unstructured-extraction/from-webpage (slurp "resources/test/html/www.nber.org-papers-w22307.xyz") "http://www.nber.org/papers/w22307.xyz")
          "10.3386/w22307"))))


; 