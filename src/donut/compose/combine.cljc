(ns donut.compose.combine)

(defn >f
  "combinator that flips first two args to a function. like haskell flip

  the `>` is meant as a mnemonic for this arg swapping: `sort` sorts ascending
  by default but you can reverse the order with `>`"
  [f]
  (fn arg-swapped
    ([a b] (f b a))
    ([a b & rest] (apply f b a rest))))

(defn ||
  "combinator that calls f1 then f2. mnemonic from shell ||"
  [f1 f2]
  (fn [& args]
    (apply f1 args)
    (apply f2 args)))

(def >|| (>f ||))

(defn &&
  "combinator that calls f1, then f2 if f1 is truthy. mnemonic from shell &&"
  [f1 f2]
  (fn [& args]
    (and
     (apply f1 args)
     (apply f2 args))))

(def >&& (>f &&))

(def >merge (>f merge))
(def >into (>f into))
