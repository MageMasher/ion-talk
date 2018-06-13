(ns iontalk.message
  (:require
   [clojure.data.json :as json]
   [iontalk.responder :as responder]
   [datomic.client.api :as d]
   [iontalk.ion :as ion]
   [ring.util.response :as ring-resp]
   [ring.middleware.params :as ring-middleware]
   [ring.middleware.keyword-params :as kw-middleware]
   [datomic.ion.lambda.api-gateway :as apigw]))


(defn format-response
  [body]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
         <Response>
           <Message>
             " body "
           </Message>
         </Response>"))

(defn handler*
  [params]
  (-> params
      (assoc :conn (ion/get-connection)
             :db (d/db (ion/get-connection)))
      responder/respond
      format-response))

(defn handler
  [{:keys [input]}]
  (-> input
      (json/read-str :key-fn keyword)
      handler*))

(defn handler-web*
  [{:keys [params] :as req}]
  (-> (ring-resp/response
       (handler* params))
      (ring-resp/header "Content-Type" "text/xml; charset=utf-8")))


(def handler-web
  (apigw/ionize (-> handler-web* ring-middleware/wrap-params kw-middleware/wrap-keyword-params)))

