(ns donut.compose-test
  (:require [donut.compose :as dc]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest compose-test
  (is (= {}
         (dc/compose {} {})))

  (is (= {:foo :bar}
         (dc/compose {} {[:foo] :bar})))

  (is (= {:foo [:bar :baz]}
         (dc/compose {:foo [:bar]}
                     {[:foo] (dc/conj :baz)})))

  (is (= {:a {:b {:c '(3 2 1)}}}
         (dc/compose {:a nil}
                     {[:a :b :c] (dc/into [1 2 3])})))

  (is (= {:a {:b 1
              :c 2
              :d 3}}
         (dc/compose {:a {:b 1}}
                     {[:a] (dc/merge {:c 2 :d 3})}))))

(deftest or-test
  (is (= {:a :original}
         (dc/compose {:a :original}
                     {[:a] (dc/or :replacement)}))))

;; >f versions of functions reverse the first two arguments
(deftest compose-with->f-test
  ;; left side of merge gets merged into right side
  ;; {:b 1} gets merged into {:b 4 :c 2}
  (is (= {:a {:b 1
              :c 2}}
         (dc/compose {:a {:b 1}}
                     {[:a] (dc/>merge {:b 4 :c 2})})))

  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     {[:foo] (dc/>into [4 5 6])}))))

(deftest map->composable-test
  (is (= {[:a] 1}
         (dc/map->composition {:a 1})))

  (is (= {[:a :b :c] :d}
         (dc/map->composition {:a {:b {:c :d}}})))

  (is (= {[:a :b :c] :d
          [:a :b :e] {}
          [:x]       []
          [:y]       {}}
         (dc/map->composition {:a {:b {:c :d
                                       :e {}}}
                               :x []
                               :y {}}))))

(deftest compose-with-map->composable-test
  (is (= {:a {:b 1
              :c 2}}
         (dc/compose {:a {:b 1}}
                     (dc/map->composition
                      {:a (dc/>merge {:b 4 :c 2})}))))

  (is (= {:foo [4 5 6 1 2 3]}
         (dc/compose {:foo [1 2 3]}
                     (dc/map->composition
                      {:foo (dc/>into [4 5 6])}))))

  (is (= {:a {:b {:c {}}}
          :d {:e [4 5 6 1 2 3]}}
         (dc/compose {:a {:b {:c :going-awway}}
                      :d {:e [1 2 3]}}
                     (dc/map->composition
                      {:a {:b {:c {}}}
                       :d {:e (dc/>into [4 5 6])}})))))


;; data-oriented program involves describing what's to be done as much as
;; possible with data, deferring execution of that until later
;;
;; as a result, "composition" often happens through data composition, but doing
;; that is always kind of tricky
;;
;; value prop of this library is to provide a clear and intuitive means of data composition:
;; - "deep merge" - recursively merge arbitrarily nested data structures
;; - flexbility around how you handle merging any particular node using update semantics

(dc/compose
 {:a {:b 1}}
 {[:a] (dc/merge {:c 2 :d 3})})
;; =>
{:a {:b 1
     :c 2
     :d 3}}

;; use into to merge two values
(dc/compose
 {:a {:b {:c :going-awway}}
  :d {:e [1 2 3]}}
 (dc/map->composition
  {:a {:b {:c {}}}
   :d {:e (dc/into [4 5 6])}}))
;; =>
{:a {:b {:c {}}}
 :d {:e [1 2 3 4 5 6]}}

;; use >into, which swaps the first two args, to merge two values
(dc/compose
 {:a {:b {:c :going-awway}}
  :d {:e [1 2 3]}}
 (dc/map->composition
  {:a {:b {:c {}}}
   :d {:e (dc/>into [4 5 6])}}))
;; =>
{:a {:b {:c {}}}
 :d {:e [4 5 6 1 2 3]}}
(into [4 5 6] [1 2 3])
