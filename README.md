[![Clojars Project](https://img.shields.io/clojars/v/party.donut/compose.svg)](https://clojars.org/party.donut/compose)

# Clear and flexible data composition

``` clojure
(require '[donut.compose :as dc])

;; acts like a deep merge
(dc/compose
 {:http {:port  7000
         :join? false}}
 {:http {:port 9000}})
;; =>
{:http {:port  9000
        :join? false}}

;; use updaters on the right side to functionally transform the left side
(dc/compose
 {:http {:port  7000
         :join? false}}
 {:http {:port  (dc/or 9000)       ;; like calling (or 7000 9000)
         :join? (dc/update not)}}) ;; like calling (not false)
;; =>
{:http {:port  7000
        :join? true}}

;; all updaters
(dc/compose
 {:into     [1 2 3]
  :>into    [1 2 3]
  :merge    {:a 1, :b 1}
  :>merge   {:a 1, :b 1}
  :conj     [:x]
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
  :update-1 (dc/update inc)
  :update-2 (dc/update merge {:b 2})
  :or       (dc/or :b)
  :map      (dc/map inc)
  :mapv     (dc/mapv inc)})
;; =>
{:into     [1 2 3 4 5 6] ;; (into [1 2 3] [4 5 6])
 :>into    [4 5 6 1 2 3] ;; (into [4 5 6] [1 2 3])
 :merge    {:a 1, :b 2}  ;; (merge {:a 1, :b 1} {:b 2})
 :>merge   {:b 1, :a 1}  ;; (merge {:b 2} {:a 1, :b 1})
 :conj     [:x :y]       ;; (conj [:x] :y)
 :update-1 2             ;; (inc 1)
 :update-2 {:a 1, :b 2}  ;; (merge {:a 1} {:b 2})
 :or       :a            ;; (or :a :b)
 :mapv     [2 3 4]       ;; (map inc [1 2 3])
 :map      '(2 3 4)}     ;; (mapv inc [1 2 3])
```

# explain

`donut.compose/compose` takes two arguments, `base` and `updates`. It deep
merges the two: every leaf value in `updates` is `assoc-in`'d to the
corresponding path in `base`. This:

``` clojure
(dc/compose
 {:http {:port  7000, :join? false}}
 {:http {:port 9000}})
```

is equivalent to this:

``` clojure
(assoc-in {:http {:port  7000, :join? false}} [:http :port] 9000)
```

You can use _updaters_ as leaf values in the `updates` map to apply a function
to the `base` value:

``` clojure
(dc/compose
 {:app {:whitelist ["a" "b" "c"]}}
 {:app {:whitelist (dc/into ["d" "e" "f"])}})

;; =>
{:app {:whitelist ["a" "b" "c" "d" "e" "f"]}}
```

The above is equivalent to:

``` clojure
(update-in {:app {:whitelist ["a" "b" "c"]}} [:app :whitelist] into ["d" "e" "f"])
```

which hopefully explains why they're called "updaters".

The `dc/into` and `dc/merge` updaters have complements, `dc/>into` and
`dc/>merge`, which reverse the first two arguments. You would use `dc/>merge` if
you want `base` values to take precedence over `updates` values:

``` clojure
(dc/compose
 {:alert-service {:recipient "team@bigco.com"}}
 {:alert-service (dc/>merge {:recipient "fallback@bigco.com"})})
;; =>
{:alert-service {:recipient "team@bigco.com"}}
```

`dc/merge`, `dc/into`, etc are nice and should cover most use cases. If you need
to do something more exotic, `dc/update` opens the door for any arbitrary
transformation.

# path syntax

You can also use a path syntax:

``` clojure
(dc/compose
 {:http {:port 9000, :join? false}}
 ^::dc/path-updates
 {[:http :port] 7000})
;; =>
{:http {:port 7000, :join? false}}
```

# why

The donut project as a whole aspires to provide a framework for Clojure web
development. It takes a data-oriented approach, where the behavior of donut
libraries is described with data as much as possible. This is the best possible
way to provide default behavior which can be extended, overridden, or configured
by library consumers.

There are many areas where donut provides default configuration, and I want to
have a clear and consistent way for working with that. For example, UI
components might have default configuration that looks like this:

``` clojure
{:class ["donut-input" "donut-active"]
 :on    {:success [default-success-handler]
         :fail    [default-fail-handler]}}
```

Developers need to be able to clearly and concisely either override or build on
these default values. For example, a dev might want to preserve the default
classes while adding additional classes, and also change the `:success` handler
while adding an additional `:fail` handler. They could do that by passing in a map to the
component funtion like this:

``` clojure
{:class (dc/into ["px-1"])
 :on {:success [success-handler]
      :fail (dc/into [additional-fail-handler])}}
```

