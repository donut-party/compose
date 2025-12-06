(ns donut.compose-test
  (:require [donut.compose :as dc]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is] :include-macros true])))

;;---
;; Testing using path syntax
;;---


(deftest compose-test
  (is (= {} (dc/compose {} {})))

  (is (= {:foo :bar}
         (dc/compose {} {[:foo] :bar})))

  (is (= {:foo [:bar :baz]}
         (dc/compose {:foo [:bar]}
                     {[:foo] (dc/conj :baz)})))

  ;; calls (into nil [1 2 3])
  (is (= {:a {:b {:c '(3 2 1)}}}
         (dc/compose {:a nil}
                     {[:a :b :c] (dc/into [1 2 3])})))

  (is (= {:a {:b {:c [1 2 3]}}}
         (dc/compose {:a {:b {:c []}}}
                     {[:a :b :c] (dc/into [1 2 3])})))

  ;; default behavior is to replace value at path:
  ;; the map {:c 2 :d 3} replaces the map {:b 1}
  (is (= {:a {:c 2, :d 3}}
         (dc/compose {:a {:b 1}}
                     {[:a] {:c 2, :d 3}})))

  ;; use the merge updater for merging
  (is (= {:a {:b 1, :c 2, :d 3}}
         (dc/compose {:a {:b 1}}
                     {[:a] (dc/merge {:c 2, :d 3})}))))

;; use the or updater to only "set" a value if it's not already present
(deftest or-test
  (is (= {:a :original}
         (dc/compose {:a :original}
                     {[:a] (dc/or :replacement)}))))

;; >f versions of functions reverse the first two arguments
(deftest compose-with->f-test
  ;; left side of merge gets merged into right side
  ;; {:b 1} gets merged into {:b 4, :c 2}
  (is (= {:a {:b 1, :c 2}}
         (dc/compose {:a {:b 1}}
                     {[:a] (dc/>merge {:b 4, :c 2})})))

  ;; calls (into [4 5 6] [1 2 3])
  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
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

;; you can add ^::dc/map-updates to a map and it'll be passed through
;; dc/map->updates first
(deftest compose-with-map-updates-metadata-test
  (is (= {:a {:b 1
              :c 2}}
         (dc/compose {:a {:b 1}}
                     ^::dc/map-updates
                     {:a (dc/>merge {:b 4 :c 2})})))

  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     ^::dc/map-updates
                     {:foo (dc/>into [4 5 6])})))

  (is (= {:a {:b {:c {}}}
          :d {:e [4 5 6 1 2 3]}}
         (dc/compose {:a {:b {:c :going-awway}}
                      :d {:e [1 2 3]}}
                     ^::dc/map-updates
                     {:a {:b {:c {}}}
                      :d {:e (dc/>into [4 5 6])}}))))

;; you can use metadata to indicate use of some updaters
(deftest compose-with-metadata-updaters-test
  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:into   [1 2 3]
                      :>into  [1 2 3]
                      :merge  {:a 1}
                      :>merge {:a 1}
                      :conj [:x]
                      :>conj :x
                      :assoc {:a 1}
                      }
                     ^::dc/map-updates
                     {:foo ^::dc/>into [4 5 6]}))))
