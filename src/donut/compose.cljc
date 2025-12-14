(ns donut.compose
  (:require
   [clojure.core :as clj])
  (:refer-clojure :exclude [update merge into conj dissoc map mapv or]))

(defn >f
  "combinator that swaps first two args to a function.

  the `>` is meant as a mnemonic for this arg swapping: `sort` sorts ascending
  by default but you can reverse the order with `>`"
  [f]
  (fn arg-swapped
    ([a b] (f b a))
    ([a b & rest] (apply f b a rest))))

(defn updater
  [f]
  (fn [& args]
    {::update-f f
     ::args     args}))

;;---
;; updaters
;;---

(defn orf
  "or as a function so that it can be treated as a value"
  [& args]
  (some identity args))

(def or
  "or updater. use this when you want to prefer the left side of a compose"
  (updater orf))

(def merge  (updater clj/merge))
(def >merge (updater (>f clj/merge)))
(def into   (updater clj/into))
(def >into  (updater (>f clj/into)))
(def conj   (updater clj/conj))
(def dissoc (updater clj/dissoc))
(def map    (updater (>f clj/map)))
(def mapv   (updater (>f clj/mapv)))

(defn update
  "update updater"
  [f & args]
  (apply (updater f) args))

;;---
;; composing
;;---

(defn map->updates
  "helper that converts a map to the updates form needed to apply updates. lets
  you write your updates in the shape of the base structure if you're into that
  kind of thing."
  [m]
  (loop [updates         {}
         remaining-paths (clj/mapv vector (keys m))]
    (let [[current-path & new-remaining-paths] remaining-paths
          current-value                        (get-in m current-path)
          current-map?                         (map? current-value)]
      (cond
        (empty? remaining-paths)
        (with-meta updates {::path-updates true})

        (clj/or (not current-map?)
                (and current-map?
                     (clj/or (empty? current-value)
                             (::update-f current-value))))
        (recur (clj/assoc updates current-path current-value)
               new-remaining-paths)

        :else
        (recur updates
               (clj/into (clj/mapv (fn [k] (clj/conj current-path k))
                                   (keys current-value))
                         new-remaining-paths))))))

(defn apply-update
  [base path update-val]
  (if-let [update-function (::update-f update-val)]
    (update-in base path (fn [x] (apply update-function x (::args update-val))))
    (assoc-in base path update-val)))

(defn compose
  "updatey-merge of two values"
  [base updates]
  (reduce-kv apply-update
             base
             (cond-> updates
               (not (-> updates meta ::path-updates)) map->updates)))

(def >compose (>f compose))

(defn composable
  "returns a function that provides a nice interface for point composing. useful for hiccup"
  [updates]
  (fn composable-fn
    ([k] (composable-fn k nil))
    ([k base]
     (-> {k base}
         (compose updates)
         (get k)))))
