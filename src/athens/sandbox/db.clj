(ns ^{:doc "Helpers to quickly explore the athens database from a Clojure REPL"}
    athens.sandbox.db
  (:require [datascript.core :as d]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

;; Source: `src/cljs/athens/db.clj` on athensresearch/athens:master

(def athens-url "https://raw.githubusercontent.com/athensresearch/athens/master/data/athens.datoms")
(def help-url   "https://raw.githubusercontent.com/athensresearch/athens/master/data/help.datoms")
(def ego-url    "https://raw.githubusercontent.com/athensresearch/athens/master/data/ego.datoms")

(defn parse-tuples
  "Parse tuples exported via method specified in https://roamresearch.com/#/app/ego/page/eJ14YtH2G."
  [tuples]
  (->> tuples
       (partition 3)            ; chunk into 3-tuples
       rest                     ; drop first tuple which is (?e ?a ?v)
       (map #(map edn/read-string %))
       (map #(cons :db/add %))))

(def schema
  {:block/uid      {:db/unique :db.unique/identity}
   :node/title     {:db/unique :db.unique/identity}
   :attrs/lookup   {:db/cardinality :db.cardinality/many}
   :block/children {:db/cardinality :db.cardinality/many
                    :db/valueType :db.type/ref}})

(defn slurp-json-db [uri]
  (let [conn (d/create-conn schema)
        txs (-> athens-url
                slurp
                json/read-str
                parse-tuples)]
    (d/transact! conn txs)
    @conn))

(comment
  (defonce my-db (slurp-json-db athens-url))

  (type my-db)
  ;; => datascript.db.DB

  (d/q '{:find [(count ?t) .]
         :where [[_ :node/title ?t]]}
       my-db)
  ;; => 257
  )
