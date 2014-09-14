(ns defun
  "defun: A beautiful macro to define clojure functions.
    author:  dennis <killme2008@gmail.com>"
  (:use [clojure.core.match :only (match)]
        [clojure.walk :only [postwalk]]))

(defn- ^{:dynamic true :doc "From clojure.core, but it's private, so we
  have to copy it."}
  assert-valid-fdecl
  "A good fdecl looks like (([a] ...) ([a b] ...)) near the end of defn."
  [fdecl]
  (when (empty? fdecl) (throw (IllegalArgumentException.
                                "Parameter declaration missing")))
  (let [argdecls (map
                   #(if (seq? %)
                      (first %)
                      (throw (IllegalArgumentException.
                        (if (seq? (first fdecl))
                          (str "Invalid signature "
                               %
                               " should be a list")
                          (str "Parameter declaration "
                               %
                               " should be a vector")))))
                   fdecl)
        bad-args (seq (remove #(vector? %) argdecls))]
    (when bad-args
      (throw (IllegalArgumentException. (str "Parameter declaration " (first bad-args)
                                             " should be a vector"))))))


(def
 ^{:private true :doc "From clojure.core,but is private,so we have to copy it."}
 sigs
 (fn [fdecl]
   (assert-valid-fdecl fdecl)
   (let [asig
         (fn [fdecl]
           (let [arglist (first fdecl)
                 ;elide implicit macro args
                 arglist (if (clojure.lang.Util/equals '&form (first arglist))
                           (clojure.lang.RT/subvec arglist 2 (clojure.lang.RT/count arglist))
                           arglist)
                 body (next fdecl)]
             (if (map? (first body))
               (if (next body)
                 (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                 arglist)
               arglist)))]
     (if (seq? (first fdecl))
       (loop [ret [] fdecls fdecl]
         (if fdecls
           (recur (conj ret (asig (first fdecls))) (next fdecls))
           (seq ret)))
       (list (asig fdecl))))))


(defmacro defun
  [name & fdecl]
  "Define a function just like defn,but using core.match to match parameters.
   See https://github.com/killme2008/defun for details."
  ;;processing forms in the same way with defn,
  ;;the let form below is from clojure.core/defn
  (let [m (if (string? (first fdecl))
            {:doc (first fdecl)}
            {})
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        m (if (map? (last fdecl))
            (conj m (last fdecl))
            m)
        fdecl (if (map? (last fdecl))
                (butlast fdecl)
                fdecl)
        m (conj {:arglists (list 'quote (sigs fdecl))} m)
        m (let [inline (:inline m)
                ifn (first inline)
                iname (second inline)]
            ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
            (if (if (clojure.lang.Util/equiv 'fn ifn)
                  (if (instance? clojure.lang.Symbol iname) false true))
              ;; inserts the same fn name to the inline fn if it does not have one
              (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                               (next inline))))
              m))
        m (conj (if (meta name) (meta name) {}) m)
        fdecl (postwalk
               (fn [form]
                 (if (and (list? form) (= 'recur (first form)))
                   (list 'recur (cons 'vector (next form)))
                   form)) fdecl)]
    `(defn ~name ~m [ & args#]
       (match [(vec args#)]
              ~@(mapcat
                 (fn [form]
                   [[(first form)] (cons 'do (next form))])
                 fdecl)))))
