(ns iontalk.message
  (:require
   [clojure.data.json :as json]
   [ring.util.response :as ring-resp]
   [ring.middleware.params :as ring-middleware]
   [datomic.ion.lambda.api-gateway :as apigw]))


(defn- handler*
  [params]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
         <Response>
           <Message>
             " params "
           </Message>
         </Response>"))

(defn handler
  [{:keys [input]}]
  (-> input
      (json/read-str :key-fn keyword)
      :params
      handler*))

(defn- handler-web*
  [{:keys [params] :as req}]
  (-> (ring-resp/response
       (handler* params))
      (ring-resp/header "Content-Type" "text/xml; charset=utf-8")))


(def handler-web
  (apigw/ionize (-> handler-web* ring-middleware/wrap-params)))

