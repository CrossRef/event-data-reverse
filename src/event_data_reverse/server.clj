(ns event-data-reverse.server
  (:require [event-data-reverse.lookup :as lookup]
            [overtone.at-at :as at-at]
            [event-data-common.status :as status]
            [clojure.tools.logging :as log])
  (:require [compojure.core :refer [context defroutes GET ANY POST]]
            [compojure.handler :as handler]
            [compojure.route :as route])
  (:require [liberator.core :refer [defresource resource]]
            [ring.util.response :refer [redirect]]
            [liberator.representation :refer [ring-response]])
  (:require [selmer.parser :refer [render-file cache-off!]]
            [selmer.filters :refer [add-filter!]])
  (:require [clojure.data.json :as json])
  (:require [crossref.util.doi :as crdoi])
  (:require [config.core :refer [env]])
  (:require [org.httpkit.server :as server]))


(def event-data-homepage "http://eventdata.crossref.org/guide")

(defresource home
  []
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
                (ring-response
                  (redirect event-data-homepage))))

(defresource guess-doi
  []
  :available-media-types ["text/plain"]
  :malformed? (fn [ctx]
                (let [input (get-in ctx [:request :params :q])]
                  (when-not input
                    (log/info "Malformed:" input))
                  [(not input) {::input input}]))
  :exists? (fn [ctx]
            (let [[method doi] (lookup/lookup (::input ctx))]
              [doi {::doi doi ::method method}]))
  :handle-ok (fn [ctx]
               (log/info "OK:" (::input ctx) "->" (::doi ctx))
               (ring-response {:body (::doi ctx)
                               :headers {"X-Method" (name (::method ctx))
                                         "X-Version" (System/getProperty "event-data-reverse.version")}})))

(defroutes app-routes
  (GET "/" [] (home))
  (GET "/guess-doi" [] (guess-doi)))

(def app
  (-> app-routes
      handler/site))

(def schedule-pool (at-at/mk-pool))

(defn start []
  (let [port (Integer/parseInt (:port env))]
    (log/info "Start heartbeat")
    (at-at/every 10000 #(status/send! "reverse" "heartbeat" "tick" 1) schedule-pool)
   
    (log/info "Start server on " port)
    (server/run-server app {:port port})))
