# defun

A beautiful macro to define clojure functions.

## Usage

Dependency in leiningen:

```clj
    [defun "0.1.0-SNAPSHOT"]
```

### Basic usage

Use `defun` in your namespace to define function just like `defn`:

```clj
(use '[defun :only [defun]])

(defun hello [name] (str "hello," name))
(hello "defun")
;; "hello,defun"
```

Funny is coming, let's say hi to people:

```clj
(defun say-hi
  ([:dennis] "Hi,good monirning, dennis.")
  ([:catty] "Hi, catty, what time is it?")
  ([:green] "Hi,green, what a good day!")
  ([other] (str "Say hi to " other)))
```

Then calling `say-hi` with different names:

```clj
(say-hi :dennis)
;;  "Hi,good monirning, dennis."
(say-hi :catty)
;;  "Hi, catty, what time is it?"
(say-hi :green)
;;  "Hi,green, what a good day!"
(say-hi "someone")
;;  "Say hi to someone"
```
We define functions just like Erlang's function with parameters pattern match (thanks to [core.match](https://github.com/clojure/core.match)), we don't need `if,cond,case` any more, that's cool!

### Recursion

Let's move on, what about define a recursion function? That's easy too:

```clj
(defun count-down
  ([0] (println "Reach zero!"))
  ([n] (println n)
     (recur (dec n))))
```

Invoke it:

```clj
(count-down 5)
;;5
;;4
;;3
;;2
;;1
;;Reach zero!
nil
```

A fibonacci function:

```clj
(defun fib
    ([0] 0)
    ([1] 1)
   ([n] (+ (fib (- n 1)) (fib (- n 2)))))
```

Output:

```clj
(fib 10)
;; 55
```

Of course it's not tail recursive, but it's really cool, isn't it?

## Guards

Added a guard function to a parameter:

```clj
(defun funny
  ([(N :guard #(= 42 %))] true)
  ([_] false))

(funny 42)
;;  true
(funny 43)
;; false
```
A function to detect if longitude  and latitude values are both valid:

```
(defun valid-geopoint?
    ([(_ :guard #(and (> % -180) (< % 180)))
      (_ :guard #(and (> % -90) (< % 90)))] true)
    ([_ _] false))

(valid-geopoint? 30 30)
;; true
(valid-geopoint? -181 30)
;; false
```

## More Patterns

In fact ,the above `say-hi` function will be expanded to be:

```clj
(defn
 say-hi
 {:arglists '([& args])}
 [& arguments*]
 (clojure.core.match/match
  [(vec arguments*)]
  [[:dennis]]
  (do "Hi,good monirning, dennis.")
  [[:catty]]
  (do "Hi, catty, what time is it?")
  [[:green]]
  (do "Hi,green, what a good day!")
  [[other]]
  (do (str "Say hi to " other))))
```

The argument vector is a pattern in core.match, so we can use all patterns that supported by [core.match](https://github.com/clojure/core.match/wiki/Basic-usage).

For example, matching literals

```clj
(defun test1
    ([true false] 1)
    ([true true] 2)
    ([false true] 3)
    ([false false] 4))

(test1 true true)
;; 2
(test1 false false)
;; 4
```

Matching sequence:

```clj
(defun test2
    ([([1] :seq)] :a0)
    ([([1 2] :seq)] :a1)
    ([([1 2 nil nil nil] :seq)] :a2))

(test2 [1 2 nil nil nil])
;; a2
```

Matching vector:

```clj
(defun test3
    ([[_ _ 2]] :a0)
    ([[1 1 3]] :a1)
    ([[1 2 3]] :a2))

(test3 [1 2 3])
;; :a2
```

Rest Pattern, Map Pattern, Or Pattern etc.

I don't want to copy the [core.match's wiki](https://github.com/clojure/core.match/wiki/Basic-usage),please visit it by yourself.


## License

Copyright © 2014 [Dennis Zhuang](mailto:killme2008@gmail.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
