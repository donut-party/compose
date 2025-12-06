(ns donut.compose
  (:require
   [clojure.core :as clj]
   [donut.compose.macros :refer [defupdater]])
  (:refer-clojure
   :exclude
   [update merge into conj assoc map mapv or]))

(declare
 update  merge  into  conj  assoc  map  mapv
 >update >merge >into >conj >assoc >map >mapv >f)

(defn >f
  [f]
  (fn arg-swapped
    ([a b] (f b a))
    ([a b & rest] (apply f b a rest))))

(defn updater
  [f]
  (fn [& args]
    {:donut.compose/composing-function f
     :donut.compose/args               args}))

(defn >updater
  [f]
  (fn [& args]
    {:donut.compose/composing-function (>f f)
     :donut.compose/args               args}))

(defupdater update)
(defupdater merge)
(defupdater into)
(defupdater conj)
(defupdater assoc)
(defupdater map)
(defupdater mapv)

(defn orf
  [& args]
  (some identity args))

(def or (updater orf))

(defn compose-update
  [base path update-val]
  (if-let [composing-function (::composing-function update-val)]
    (update-in base path (fn [x] (apply composing-function x (::args update-val))))
    (assoc-in base path update-val)))

(defn compose
  [base composition]
  (reduce-kv compose-update base composition))

(defn map->composition
  [m]
  (loop [composable      {}
         remaining-paths (clj/mapv vector (keys m))]
    (let [[current-path & new-remaining-paths] remaining-paths
          current-value                        (get-in m current-path)
          current-map?                         (map? current-value)]
      (cond
        (empty? remaining-paths)
        composable

        (clj/or (not current-map?)
                (and current-map?
                     (clj/or (empty? current-value)
                             (::composing-function current-value))))
        (recur (clj/assoc composable current-path current-value)
               new-remaining-paths)

        :else
        (recur composable
               (clj/into
                (clj/mapv (fn [k] (clj/conj current-path k))
                          (keys current-value))
                new-remaining-paths))))))

;; TODO composer for or
;; TODO rename composer to updater?
