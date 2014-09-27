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
  [name & fdecl]
  (let [[name body] (name-with-attributes name fdecl)
        body (if (vector? (first body))
               (list body)
               body)
        body (postwalk
               (fn [form]
                 (if (and (list? form) (= 'recur (first form)))
                   (list 'recur (cons 'vector (next form)))
                   form))
               body)]
    `(defn ~name [& args#]
       (match (vec args#)
              ~@(mapcat
                 (fn [[m & more]]
                   [m (cons 'do more)])
                 body)))))

(defmacro defun-
  "same as defun, yielding non-public def"
  [name & decls]
  (list* `defun (vary-meta name assoc :private true) decls))
