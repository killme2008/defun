(ns
    ^{:author "dennis <killme2008@gmail.com>"
      :doc "A macro to define clojure functions with parameter pattern matching
            just like erlang or elixir based on core.match. Please see
            https://github.com/killme2008/defun"}
  defun.core
  (:require #?(:clj [clojure.core.match]
               :cljs [cljs.core.match :include-macros true])
            #?@(:clj [[clojure.tools.macro :refer [name-with-attributes]]
                      [clojure.walk :refer [postwalk]]]))
  #?(:cljs (:require-macros [defun.core :refer [fun letfun defun defun-]])))

#?(:clj
   (defmacro if-cljs
     "Return then if we are generating cljs code and else for clj code.
     Source:
     http://blog.nberger.com.ar/blog/2015/09/18/more-portable-complex-macro-musing/"
     [then else]
     (if (boolean (:ns &env)) then else)))

#?(:clj
   (defmacro match
     [& args]
     `(if-cljs (cljs.core.match/match ~@args)
               (clojure.core.match/match ~@args))))

(def placeholder (Object.))

#?(:clj
   (defmacro fun*
     "Defines a function just like clojure.core/fn with parameter pattern matching
     See https://github.com/killme2008/defun for details."
     [& sigs]
     {:forms '[(fun name? [params* ] exprs*) (fun name? ([params* ] exprs*)+)]}
     (let [name (when (symbol? (first sigs)) (first sigs))
           sigs (if name (next sigs) sigs)
           name (or name (gensym "fn__"))
           sigs (if (vector? (first sigs))
                  (list sigs)
                  (if (seq? (first sigs))
                    (let [sig-args (map first sigs)]
                      (if-some [[form] (some #(if-not (vector? %) [%]) sig-args)]
                        (throw (IllegalArgumentException.
                                (if (some? form)
                                  (str "Parameter declaration "
                                       form
                                       " should be a vector")
                                  (str "Parameter declaration missing"))))
                        sigs))
                    ;; Assume single arity syntax
                    (throw (IllegalArgumentException.
                            (if (seq sigs)
                              (str "Parameter declaration "
                                   (first sigs)
                                   " should be a vector")
                              "Parameter declaration missing")))))
           sigs (sort-by #(count (first %)) sigs)
           args (map first sigs)
           arities (set (map count args))
           max-arity (apply max arities)
           other-arities (disj arities max-arity)
           placeholder-sym (gensym "placeholder")
           placeholder-syms (cons placeholder-sym (repeat '_))
           args
           (map #(into % (take (- max-arity (count %)) placeholder-syms)) args)
           bodies (map #(cons 'do %) (map rest sigs))
           bodies (postwalk
                   (fn [form]
                     (if (and (list? form) (= 'recur (first form)))
                       (let [recur-args (next form)
                             recur-args-n (count recur-args)
                             to-add-n (- max-arity recur-args-n)
                             tail (take to-add-n (repeat `placeholder))]
                         (cons 'recur (concat recur-args tail)))
                       form))
                    bodies)
           statements (interleave args bodies)
           args* (vec (take (count (first args)) (repeatedly gensym)))
           main-sig (list
                     args*
                     (list
                      `let [placeholder-sym `placeholder]
                      (list* `match args* statements)))
           other-sigs (map (fn [a]
                             (let [args (subvec args* 0 a)
                                   to-add-n (- max-arity a)
                                   tail (take to-add-n (repeat `placeholder))]
                               (list args (cons name (concat args tail)))))
                       other-arities)
           all-sigs (concat other-sigs [main-sig])]
       (list* `fn name all-sigs))))

(defn variadic? [sigs]
  (->> sigs
    (map first)
    (map (partial some #{'&}))
    (filter some?)
    (seq)
    (boolean)))

#?(:clj
   (defmacro fun
     "Defines a function just like clojure.core/fn with parameter pattern matching
     See https://github.com/killme2008/defun for details."
     [& sigs]
     {:forms '[(fun name? [params* ] exprs*) (fun name? ([params* ] exprs*)+)]}
     (let [name (when (symbol? (first sigs)) (first sigs))
           sigs (if name (next sigs) sigs)
           sigs (if (vector? (first sigs))
                  (list sigs)
                  (if (seq? (first sigs))
                    (let [sig-args (map first sigs)]
                      (if-some [[form] (some #(if-not (vector? %) [%]) sig-args)]
                        (throw (IllegalArgumentException.
                                 (if (some? form)
                                   (str "Parameter declaration "
                                     form
                                     " should be a vector")
                                   (str "Parameter declaration missing"))))
                        sigs))
                    ;; Assume single arity syntax
                    (throw (IllegalArgumentException.
                             (if (seq sigs)
                               (str "Parameter declaration "
                                 (first sigs)
                                 " should be a vector")
                               "Parameter declaration missing")))))]
       (if (variadic? sigs)
         (let [sigs (postwalk
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
                        sigs)))
         `(fun* ~@sigs)))))

#?(:clj
   (defmacro letfun
     "letfn with parameter pattern matching."
     {:forms '[(letfun [fnspecs*] exprs*)]}
     [fnspecs & body]
     `(letfn* ~(vec (interleave (map first fnspecs)
                                (map #(cons `fun %) fnspecs)))
              ~@body)))

#?(:clj
   (defmacro defun
     "Define a function just like clojure.core/defn, but using core.match to
     match parameters. See https://github.com/killme2008/defun for details."
     [name & fdecl]
     (let [[name body] (name-with-attributes name fdecl)
           body (if (vector? (first body))
                  (list body)
                  body)
           name (vary-meta name assoc :arglists (list 'quote (@#'clojure.core/sigs body)))]
       `(def ~name (fun ~@body)))))

#?(:clj
   (defmacro defun-
     "same as defun, yielding non-public def"
     [name & decls]
     (list* `defun (vary-meta name assoc :private true) decls)))
