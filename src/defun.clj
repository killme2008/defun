(ns
  ^{:author "dennis <killme2008@gmail.com>"
    :doc "A beautiful macro to define clojure functions"}
  defun
  (:require [clojure.core.match :refer [match]]
            [clojure.tools.macro :refer [name-with-attributes]]
            [clojure.walk :refer [postwalk]]))

(defmacro defun
 "Define a function just like defn, but using core.match to match parameters.
  See https://github.com/killme2008/defun for details."
  [name & pairs]
  (let [[name pairs] (name-with-attributes name pairs)
        body (postwalk
               (fn [form]
                 (if (and (list? form) (= 'recur (first form)))
                   (list 'recur (cons 'vector (next form)))
                   form))
               (partition 2 pairs))]
    `(defn ~name [& args#]
       (match (vec args#)
              ~@(apply concat body)))))

(defmacro defun-
  "same as defun, yielding non-public def"
  [name & decls]
  (list* `defun (vary-meta name assoc :private true) decls))
