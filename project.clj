(defproject event-data-reverse "0.1.0-SNAPSHOT"
  :description "Event Data Reverse"
  :url "http://eventdata.crossref.org/"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                [clj-time "0.12.2"]
                [com.cemerick/url "0.1.1"]
                [compojure "1.5.1"]
                [crossref-util "0.1.10"]
                [enlive "1.1.6"]
                [event-data-common "0.1.9"]
                [http-kit "2.1.18"]
                [http-kit.fake "0.2.1"]
                [javax/javaee-api "7.0"]
                [liberator "0.14.1"]
                [org.apache.logging.log4j/log4j-core "2.6.2"]
                [org.clojure/tools.logging "0.3.1"]
                [org.eclipse.jetty/jetty-server "9.4.0.M0"]
                [org.slf4j/slf4j-simple "1.7.21"]
                [overtone/at-at "1.2.0"]
                [ring "1.5.0"]
                [ring/ring-jetty-adapter "1.5.0"]
                [ring/ring-mock "0.3.0"]
                [ring/ring-servlet "1.5.0"]
                [robert/bruce "0.8.0"]
                [selmer "0.9.5"]
                [yogthos/config "0.8"]
                ]
  :main ^:skip-aot event-data-reverse.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
