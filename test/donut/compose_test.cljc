(ns donut.compose-test
  (:require [donut.compose :as dc]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is] :include-macros true])))

;; use ^::dc/path-updates if you prefer that form
(deftest compose-test
  (is (= {} (dc/compose {} {})))

  (is (= {:foo :bar}
         (dc/compose {}
                     ^::dc/path-updates
                     {[:foo] :bar})))

  (is (= {:foo [:bar :baz]}
         (dc/compose {:foo [:bar]}
                     ^::dc/path-updates
                     {[:foo] (dc/conj :baz)})))

  ;; calls (into nil [1 2 3])
  (is (= {:a {:b {:c '(3 2 1)}}}
         (dc/compose {:a nil}
                     ^::dc/path-updates
                     {[:a :b :c] (dc/into [1 2 3])})))

  (is (= {:a {:b {:c [1 2 3]}}}
         (dc/compose {:a {:b {:c []}}}
                     ^::dc/path-updates
                     {[:a :b :c] (dc/into [1 2 3])})))

  ;; default behavior is to replace value at path:
  ;; the map {:c 2 :d 3} replaces the map {:b 1}
  (is (= {:a {:c 2, :d 3}}
         (dc/compose {:a {:b 1}}
                     ^::dc/path-updates
                     {[:a] {:c 2, :d 3}})))

  ;; use the merge updater for merging
  (is (= {:a {:b 1, :c 2, :d 3}}
         (dc/compose {:a {:b 1}}
                     ^::dc/path-updates
                     {[:a] (dc/merge {:c 2, :d 3})}))))

;; use the or updater to only "set" a value if it's not already present
(deftest or-test
  (is (= {:a :original}
         (dc/compose {:a :original}
                     ^::dc/path-updates
                     {[:a] (dc/or :replacement)}))))

;; >f versions of functions reverse the first two arguments
(deftest compose-with->f-test
  ;; left side of merge gets merged into right side
  ;; {:b 1} gets merged into {:b 4, :c 2}
  (is (= {:a {:b 1, :c 2}}
         (dc/compose {:a {:b 1}}
                     ^::dc/path-updates
                     {[:a] (dc/>merge {:b 4, :c 2})})))

  ;; calls (into [4 5 6] [1 2 3])
  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     ^::dc/path-updates
                     {[:foo] (dc/>into [4 5 6])}))))

(deftest map->updates-test
  (is (= {[:a] 1}
         (dc/map->updates {:a 1})))

  (is (= {[:a :b :c] :d}
         (dc/map->updates {:a {:b {:c :d}}})))

  (is (= {[:a :b :c] :d
          [:a :b :e] {}
          [:x]       []
          [:y]       {}}
         (dc/map->updates {:a {:b {:c :d
                                   :e {}}}
                           :x []
                           :y {}})))

  (is (= {[:a] (dc/merge {:b :c})}
         (dc/map->updates {:a (dc/merge {:b :c})}))))

;; show composing works with values returned by dc/map->updates
(deftest compose-with-map->updates-test
  (is (= {:a {:b 1
              :c 2}}
         (dc/compose {:a {:b 1}}
                     (dc/map->updates
                      {:a (dc/>merge {:b 4 :c 2})}))))

  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     (dc/map->updates
                      {:foo (dc/>into [4 5 6])}))))

  (is (= {:a {:b {:c {}}}
          :d {:e [4 5 6 1 2 3]}}
         (dc/compose {:a {:b {:c :going-awway}}
                      :d {:e [1 2 3]}}
                     (dc/map->updates
                      {:a {:b {:c {}}}
                       :d {:e (dc/>into [4 5 6])}})))))

;; works with maps
(deftest compose-with-maps-test
  (is (= {:a {:b 1
              :c 2}}
         (dc/compose {:a {:b 1}}
                     {:a (dc/>merge {:b 4 :c 2})})))

  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     {:foo (dc/>into [4 5 6])})))

  (is (= {:a {:b {:c {}}}
          :d {:e [4 5 6 1 2 3]}}
         (dc/compose {:a {:b {:c :going-awway}}
                      :d {:e [1 2 3]}}
                     {:a {:b {:c {}}}
                      :d {:e (dc/>into [4 5 6])}}))))

(deftest updaters-test
  (is (= {:into     [1 2 3 4 5 6]
          :>into    [4 5 6 1 2 3]
          :merge    {:a 1, :b 2}
          :>merge   {:a 1, :b 1}
          :conj     [:x :y]
          :dissoc   {:a 1}
          :update-1 2
          :update-2 {:a 1, :b 2}
          :or       :a
          :map      [2 3 4]
          :mapv     [2 3 4]}
         (dc/compose {:into     [1 2 3]
                      :>into    [1 2 3]
                      :merge    {:a 1, :b 1}
                      :>merge   {:a 1, :b 1}
                      :conj     [:x]
                      :dissoc   {:a 1, :b 2}
                      :update-1 1
                      :update-2 {:a 1}
                      :or       :a
                      :map      [1 2 3]
                      :mapv     [1 2 3]}
                     {:into     (dc/into [4 5 6])
                      :>into    (dc/>into [4 5 6])
                      :merge    (dc/merge {:b 2})
                      :>merge   (dc/>merge {:b 2})
                      :conj     (dc/conj :y)
                      :dissoc   (dc/dissoc :b)
                      :update-1 (dc/update inc)
                      :update-2 (dc/update merge {:b 2})
                      :or       (dc/or :b)
                      :map      (dc/map inc)
                      :mapv     (dc/mapv inc)}))))

;; composing for hiccupy-type functions

(defn form
  [opts]
  (let [composable (dc/composable opts)]
    [:form
     [:div (composable :wrapper-opts {:class ["mx-1"]})
      [:label (composable :label-opts)
       ;; TODO this makes sense but i don't like it
       (:label-text opts "default label")]]]))

(deftest composable-test
  (is (= [:form
          [:div {:class ["mx-1"]}
           [:label nil "default label"]]]
         (form {})))
  (is (= [:form
          [:div {:class ["mx-1" "pb-1"]}
           [:label nil "my text"]]]
         (form {:wrapper-opts {:class (dc/into ["pb-1"])}
                :label-text "my text"}))))
