(ns iontalk.schema
  (:require
   [datomic.client.api :as d]
   [clojure.java.io :as io]))

(defn- has-ident?
  [db ident]
  (contains? (d/pull db {:eid ident :selector [:db/ident]})
             :db/ident))

(defn- data-loaded?
  [db]
  (has-ident? db :message/body))

(def schema-1
  (read-string (slurp (io/resource "datomic/schema.edn"))))

(defn load-dataset
  [conn]
  (let [db (d/db conn)]
    (if (data-loaded? db)
      :already-loaded
      (letfn [(xact [tx-data]
                (d/transact conn {:tx-data tx-data}))]
        (xact schema-1)
        :loaded))))
