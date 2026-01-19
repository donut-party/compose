(ns donut.compose-test
  (:require [donut.compose :as dc]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing] :include-macros true])))

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

(deftest function-updaters-test
  (let [behavior   (atom [])
        base-fn    (fn [x] (swap! behavior conj [:from-base x]) x)
        updates-fn (fn [x] (swap! behavior conj [:from-updates x]) x)]

    (let [base     {:|| base-fn}
          updates  {:|| (dc/|| updates-fn)}
          composed (dc/compose base updates)]
      ;; || true
      ((:|| composed) true)
      (is (= [[:from-base true]
              [:from-updates true]]
             @behavior))

      ;; || false
      (reset! behavior [])
      ((:|| composed) false)
      (is (= [[:from-base false]
              [:from-updates false]]
             @behavior)))

    (reset! behavior [])

    (let [base     {:>|| base-fn}
          updates  {:>|| (dc/>|| updates-fn)}
          composed (dc/compose base updates)]
      ;; >|| true
      ((:>|| composed) true)
      (is (= [[:from-updates true]
              [:from-base true]]
             @behavior))

      ;; >|| false
      (reset! behavior [])
      ((:>|| composed) false)
      (is (= [[:from-updates false]
              [:from-base false]]
             @behavior)))

    (reset! behavior [])

    (let [base     {:&& base-fn}
          updates  {:&& (dc/&& updates-fn)}
          composed (dc/compose base updates)]
      ;; && true
      ((:&& composed) true)
      (is (= [[:from-base true]
              [:from-updates true]]
             @behavior))

      ;; && false
      (reset! behavior [])
      ((:&& composed) false)
      (is (= [[:from-base false]]
             @behavior)))

    (reset! behavior [])

    (let [base     {:>&& base-fn}
          updates  {:>&& (dc/>&& updates-fn)}
          composed (dc/compose base updates)]
      ;; >&& true
      ((:>&& composed) true)
      (is (= [[:from-updates true]
              [:from-base true]]
             @behavior))

      ;; >&& false
      (reset! behavior [])
      ((:>&& composed) false)
      (is (= [[:from-updates false]]
             @behavior)))))

;; composing for hiccupy-type functions

(defn form
  [opts]
  (let [composable (dc/composable opts)]
    [:form
     [:div (composable :wrapper-opts {:class ["mx-1"]}) ;; works with a map
      [:label (composable :label-opts)                  ;; works with nil
       (composable :label-text "default label")]        ;; works with scalar
      [:input {:type :submit
               :class (composable :input-class ["p-1"])}]]])) ;; works with vector

(deftest composable-test
  ;; no customization
  (is (= [:form
          [:div
           {:class ["mx-1"]}
           [:label nil "default label"]
           [:input {:class ["p-1"], :type :submit}]]]
         (form nil)))
  ;; composing
  (is (= [:form
          [:div
           {:class ["mx-1" "pb-1"]}
           [:label {:class ["label opts"]} "my text"]
           [:input {:type :submit, :class ["mb-1" "p-1"]}]]]
         (form {:wrapper-opts {:class (dc/into ["pb-1"])}
                :label-text "my text"
                :label-opts {:class ["label opts"]}
                :input-class (dc/>into ["mb-1"])}))))

(deftest wrap-test
  (is (= [:a :b]
         (-> (dc/compose {:a (fn [] :a)}
                         {:a (dc/wrap (fn [f]
                                        (fn [] [(f) :b])))})
             :a
             ((fn [f] (f)))))))

(deftest compose-contained-test
  (testing "only composes keys contained in base"
    (is (= {:a 1}
           (dc/compose-contained {:a 1} {:b 2})))

    (is (= {:a 2}
           (dc/compose-contained {:a 1} {:a (dc/update inc)})))))
