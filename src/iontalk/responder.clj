(ns iontalk.responder
  (:require
   [datomic.client.api :as d]
   [clojure.string :as str]
   [iontalk.ion :as ion]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Text Utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn nil-when-blank
  [s]
  (when-not (str/blank? s) s))

(defn remove-begin-end-tags
  [s]
  (-> s
      (str/replace #"(?i)(\s*\#[\w-_]+\s*)+$" "")
      (str/replace #"(?i)^(\s*\#[\w-_]+\s*)+" "")))

(defn some-body
  "Normalized body text without tags, or nil if blank"
  [body]
  (-> body
      remove-begin-end-tags
      str/trim
      (str/replace #"\s+" " ")
      nil-when-blank))

(def hashtags-re #"\#([\w-_]+)")
(defn hashtags [s]
  "A seq of normalized hashtags from s"
  (when-not (empty? s)
    (map (comp str/lower-case first) (re-seq hashtags-re s))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; respond multi-methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn respond-dispatch
  [{:strs [To From Body] :as msg}]
  {:tags (set (hashtags Body))})

(defmulti respond respond-dispatch)
(defmethod respond :default [context]
  "Sorry friend, my silly bot brain can't understand this big big world.")

(defmethod respond {:tags #{"#fortune"}}
  [{:strs [db] :as context}]
  (->> (d/q
        '[:find  ?text ?author
          :where
          [?e :fortune/author ?author]
          [?e :fortune/text ?text]]
        db)
       vec
       shuffle
       first
       (str/join " -")))






















































;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fortune-Teller
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn author-match?
  [db e ?author]
  (let [{:keys [:fortune/author]} (d/pull db {:eid e :selector [:fortune/author]})]
    (= (str/lower-case author) (str/lower-case ?author))))


(defmethod respond {:tags #{"#fortune-teller"}}
  [{:strs [db Body] :as context}]
  (->> (some-body Body)
       (d/q
        '[:find ?text ?author-result
          :in $ ?author
          :where
          [?e :fortune/author ?author-result]
          [?e :fortune/text ?text]
          [(= ?match true)]
          [(iontalk.responder/author-match? $ ?e ?author) ?match]]
        db)
       vec
       shuffle
       first
       (str/join " -")))















































;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apropos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn apropos?
  [db e ?search]
  (let [{:keys [:fortune/text]} (d/pull db {:eid e :selector [:fortune/text]})]
    (boolean (str/includes? text ?search))))


(defmethod respond {:tags #{"#apropos"}}
  [{:strs [db Body] :as context}]
  (->> (some-body Body)
       (d/q
        '[:find ?text ?author
          :in $ ?search
          :where
          [?e :fortune/author ?author]
          [?e :fortune/text ?text]
          [(= ?apropos true)]
          [(iontalk.responder/apropos? $ ?e ?search) ?apropos]]
        db)
       (map #(->> % (str/join " -")))
       (str/join ", ")))










;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New Fortune
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod respond {:tags #{"#new-fortune"}}
  [context]
  (let [db (get context "db" (d/db (ion/get-connection)))]
    (d/q
     '[:find ?author (count ?text)
       :where
       [?e :fortune/author ?author]
       [?e :fortune/text ?text]]
     db)))
