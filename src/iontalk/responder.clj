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
;; Query Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn author-match?
  "Query ion exmaple. This predicate matches entities that
should be featured in a promotion."
  [db e ?author]
  (let [{:keys [:fortune/author]} (d/pull db {:eid e :selector [:fortune/author]})]
    (= (str/lower-case author) (str/lower-case ?author))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; respond multi-methods
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn respond-dispatch
  [{:keys [To From Body] :as msg}]
  {:tags (set (hashtags Body))})

(defmulti respond respond-dispatch)


(defmethod respond {:tags #{"#new-fortune"}}
  [{:keys [From Body db] :as context}]
  (d/q
       '[:find ?author (count ?text)
         :where
         [?e :fortune/author ?author]
         [?e :fortune/text ?text]]
       db))

(defmethod respond {:tags #{"#fortune"}}
  [{:keys [db] :as context}]
  (->> (d/q
       '[:find  ?text ?author
         :where
         [?e :fortune/author ?author]
         [?e :fortune/text ?text]]
       db)
      rand-nth
      (str/join " -")
      ))

(defmethod respond {:tags #{"#fortune-teller"}}
  [{:keys [db Body] :as context}]
  (d/q
       '[:find ?text ?author
         :in $ ?author
         :where
         [?e :fortune/author ?author]
         [?e :fortune/text ?text]
         [(iontalk.responder/author-match? $ ?e ?author) match?]
         [(= match? true)]]
       db
       (some-body Body)))

(defmethod respond :default [_]
  "Sorry friend, my silly bot brain can't understand this big big world.")
