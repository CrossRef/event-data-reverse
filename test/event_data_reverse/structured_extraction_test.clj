(ns event-data-reverse.structured-extraction-test
  (:require [clojure.test :refer :all]
            [event-data-reverse.structured-extraction :as structured-extraction]))


(deftest from-tags
  (testing "Can get DOIs from meta tags where plain DOI is used"
    (is (=
          (structured-extraction/from-tags (slurp "resources/test/html/www.ncbi.nlm.nih.gov-pmc-articles-PMC4852986"))
          "10.1007/s10461-013-0685-8")))

  (testing "Can get DOIs from meta tags where doi: prefix DOI is used"
    (is (=
          (structured-extraction/from-tags (slurp "resources/test/html/figshare.com-articles-A_Modeler_s_Tale-3423371-1"))
          "10.6084/m9.figshare.3423371.v1"))

    (is (=
          (structured-extraction/from-tags (slurp "resources/test/html/www.nature.com-nature-journal-vaop-ncurrent-full-nature17976"))
          "10.1038/nature17976"))

  (testing "Can get DOIs from meta tags where entity-encoded, mixed case DOI is used"
    (is (=
          (structured-extraction/from-tags (slurp "resources/test/html/onlinelibrary.wiley.com-doi-10.1002-1521-3951(200009)221-1<453--AID-PSSB453>3.0.CO;2-Q:abstract;jsessionid=FAD5B5661A7D092460BEEDA0D55204DF.f02t01"))
          ; NB lower case.
          "10.1002/1521-3951(200009)221:1<453::aid-pssb453>3.0.co;2-q")))

   (testing "Can get DOIs from spans with given class"
    (is (=
          (structured-extraction/from-tags (slurp "resources/test/html/jnci.oxfordjournals.org-content-108-6-djw160.full"))
          "10.1093/jnci/djw160")))
    ; 

    ))


