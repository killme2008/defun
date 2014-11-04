(ns
    ^{:author "dennis <killme2008@gmail.com>"
      :doc "A beautiful macro to define clojure functions"}
  defun
  (:require [clojure.core.match :refer [match]]
            [clojure.tools.macro :refer [name-with-attributes]]
            [clojure.walk :refer [postwalk]]))

(defmacro fun
  "Defines a function just like clojure.core/fn with parameter pattern matching.
   See https://github.com/killme2008/defun for details."
  [& sigs]
  {:forms '[(fun name? [params* ] exprs*) (fun name? ([params* ] exprs*)+)]}
  (let [name (when(symbol? (first sigs)) (first sigs))
        sigs (if name (next sigs) sigs)
        sigs (if (vector? (first sigs))
               (list sigs)
               (if (seq? (first sigs))
                 sigs
                 ;; Assume single arity syntax
                 (throw (IllegalArgumentException.
                         (if (seq sigs)
                           (str "Parameter declaration "
                                (first sigs)
                                " should be a vector")
                           (str "Parameter declaration missing"))))))
        sigs (postwalk
              (fn [form]
                (if (and (list? form) (= 'recur (first form)))
                  (list 'recur (cons 'vector (next form)))
                  form))
              sigs)
        sigs `([& args#]
                 (match (vec args#)
                        ~@(mapcat
                           (fn [[m & more]]
                             [m (cons 'do more)])
                           sigs)))]
    (list* 'fn (if name
                 (cons name sigs)
                 sigs))))

(defmacro letfun
  "letfn with parameter pattern matching."
  {:forms '[(letfun [fnspecs*] exprs*)]}
  [fnspecs & body]
  `(letfn* ~(vec (interleave (map first fnspecs)
                             (map #(cons `fun %) fnspecs)))
           ~@body))

(defmacro defun
  "Define a function just like clojure.core/defn, but using core.match to
  match parameters. See https://github.com/killme2008/defun for details."
  [name & fdecl]
  (let [[name body] (name-with-attributes name fdecl)
        body (if (vector? (first body))
               (list body)
               body)
        name (vary-meta name assoc :argslist (list 'quote (@#'clojure.core/sigs body)))]
    `(def ~name (fun ~@body))))

(defmacro defun-
  "same as defun, yielding non-public def"
  [name & decls]
  (list* `defun (vary-meta name assoc :private true) decls))
