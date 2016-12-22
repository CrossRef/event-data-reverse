(ns event-data-reverse.core
  (:require [event-data-reverse.server :as server]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  [& args]
  (log/info "Starting Reverse")
  (server/start))

