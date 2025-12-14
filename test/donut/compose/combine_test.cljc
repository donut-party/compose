(ns donut.compose.combine-test
  (:require [donut.compose.combine :as dcc]
            #?(:clj [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is] :include-macros true])))

(deftest ||-test
  (let [behavior (atom [])
        f1       (fn [x] (swap! behavior conj [:f1 x]))
        f2       (fn [x] (swap! behavior conj [:f2 x]))
        ||f1f2   (dcc/|| f1 f2)
        ||f2f1   (dcc/>|| f1 f2)]
    (||f1f2 true)
    (is (= [[:f1 true] [:f2 true]]
           @behavior))

    (reset! behavior [])
    (||f2f1 true)
    (is (= [[:f2 true] [:f1 true]]
           @behavior))))

(deftest &&-test
  (let [behavior (atom [])
        f1       (fn [x] (swap! behavior conj [:f1 x]) x)
        f2       (fn [x] (swap! behavior conj [:f2 x]) x)
        &&f1f2   (dcc/&& f1 f2)
        &&f2f1   (dcc/>&& f1 f2)]
    (&&f1f2 true)
    (is (= [[:f1 true] [:f2 true]]
           @behavior))

    (reset! behavior [])
    (&&f2f1 true)
    (is (= [[:f2 true] [:f1 true]]
           @behavior))

    (reset! behavior [])
    (&&f1f2 false)
    (is (= [[:f1 false]]
           @behavior))

    (reset! behavior [])
    (&&f2f1 false)
    (is (= [[:f2 false]]
           @behavior))))
