(ns iontalk.responder
  (:require
   [datomic.client.api :as d]
   [clojure.string :as str]
   [iontalk.ion :as ion]))

(def hashtags-re #"\#([\w-_]+)")
(defn hashtags [s]
  "A seq of normalized hashtags from s"
  (when-not (empty? s)
    (map (comp str/lower-case first) (re-seq hashtags-re s))))

;; (defn respond-dispatch
;;   [{:keys [To From Body] :as msg}]
;;   )

;; (defmulti respond )
