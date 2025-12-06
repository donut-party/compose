(ns donut.compose
  (:require
   [clojure.core :as clj]
   [donut.compose.macros :refer [defupdater]])
  (:refer-clojure :exclude [update merge into conj assoc map mapv or]))

(declare
 update  merge  into  conj  assoc  map  mapv
 >update >merge >into >conj >assoc >map >mapv)

(defn >f
  [f]
  (fn arg-swapped
    ([a b] (f b a))
    ([a b & rest] (apply f b a rest))))

(defn updater
  [f]
  (fn [& args]
    {::update-f f
     ::args     args}))

(defn >updater
  [f]
  (fn [& args]
    {::update-f (>f f)
     ::args     args}))

(defupdater update clj/update)
(defupdater merge clj/merge)
(defupdater into clj/into)
(defupdater conj clj/conj)
(defupdater assoc clj/assoc)
(defupdater map clj/map)
(defupdater mapv clj/mapv)

(def metadata-updaters
  {::merge  merge
   ::>merge >merge
   ::into   into
   ::>into  >into
   ::conj   conj
   ::>conj  >conj
   ::assoc  assoc
   ::>assoc >assoc})

(def metadata-updaters-set (->> metadata-updaters keys set))

(defn orf
  "or as a function so that it can be treated as a value"
  [& args]
  (some identity args))

(def or (updater orf))

(defn map->updates
  [m]
  (loop [updates         {}
         remaining-paths (clj/mapv vector (keys m))]
    (let [[current-path & new-remaining-paths] remaining-paths
          current-value                        (get-in m current-path)
          current-map?                         (map? current-value)]
      (cond
        (empty? remaining-paths)
        updates

        (clj/or (not current-map?)
                (and current-map?
                     (clj/or (empty? current-value)
                             (::update-f current-value)
                             (-> current-value meta keys metadata-updaters-set))))
        (recur (clj/assoc updates current-path current-value)
               new-remaining-paths)

        :else
        (recur updates
               (clj/into (clj/mapv (fn [k] (clj/conj current-path k))
                                   (keys current-value))
                         new-remaining-paths))))))

(defn use-meta
  [x]
  (if-let [update-f (-> (select-keys metadata-updaters
                                     (keys (meta x)))
                        first
                        second)]
    (update-f x)
    x))

(defn apply-update
  [base path update-val]
  (let [update-val (use-meta update-val)]
    (if-let [update-function (::update-f update-val)]
      (update-in base path (fn [x] (apply update-function x (::args update-val))))
      (assoc-in base path update-val))))

(defn compose
  [base updates]
  (reduce-kv apply-update
             base
             (cond-> updates
               (-> updates meta ::map-updates) map->updates)))
