(ns donut.compose.macros
  #?(:cljs (:require-macros [donut.compose.macros :refer [defupdater]])))

#?(:clj
   (defmacro defupdater
     "creates a composing function over "
     [defname f]
     `(do
        (def ~defname (donut.compose/updater ~f))
        (def ~(symbol (str ">" defname)) (donut.compose/>updater ~f)))))
