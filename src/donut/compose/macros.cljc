(ns donut.compose.macros
  #?(:cljs (:require-macros [donut.compose.macros :refer [defupdater]])))

#?(:clj
   (defmacro defupdater
     "creates a composing function over "
     [fname]
     `(do
        (defn ~fname
          [& args#]
          {:donut.compose/composing-function ~(symbol "clojure.core" (name fname))
           :donut.compose/args               args#})
        (defn ~(symbol (str ">" fname))
          [& args#]
          {:donut.compose/composing-function (donut.compose/>f ~(symbol "clojure.core" (name fname)))
           :donut.compose/args               args#}))))
