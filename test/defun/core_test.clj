(ns defun.core-test
  (:require [clojure.test :refer :all]
            [defun :refer :all]))

(deftest test-defun-square
  (testing "Normal function."
    (defun square
      "Square function"
      [x]
      (* x x))
    (is (= 4 (square 2)))
    (is (= 9 (square 3)))
    (is (thrown? IllegalArgumentException (square 3 4)))))

(deftest test-say-hi
  (testing "say-hi"
    (defun say-hi
      "Say hi to people."
      ([:dennis] "Hi,good morning, dennis.")
      ([:catty] "Hi, catty, what time is it?")
      ([:green] "Hi,green, what a good day!")
      ([other] (str "Say hi to " other)))
    (is (= "Hi,good morning, dennis." (say-hi :dennis)))
    (is (= "Hi, catty, what time is it?" (say-hi :catty)))
    (is (= "Hi,green, what a good day!" (say-hi :green)))
    (is (= "Say hi to someone" (say-hi "someone")))))

(deftest test-recursive-function
  (testing "accum"
    (defun accum
      ([0 ret] ret)
      ([n ret] (recur (dec n) (+ n ret)))
      ([n] (recur n 0)))
    (is (= 6 (accum 3)))
    (is (= 5050 (accum 100))))
  (testing "fib"
    (defun fib
      ([0] 0)
      ([1] 1)
      ([n] (+ (fib (- n 1)) (fib (- n 2)))))
    (is (= 55 (fib 10)))))

(deftest test-guards
  (testing "funny"
    (defun funny
      ([(N :guard #(= 42 %))] true)
      ([_] false))
    (is (funny 42))
    (is (not (funny 43))))
  (testing "valid-geopoint?"
    (defun valid-geopoint?
      ([(_ :guard #(and (> % -180) (< % 180)))
        (_ :guard #(and (> % -90) (< % 90)))] true)
      ([_ _] false))
    (is (valid-geopoint? 30 30))
    (is (not (valid-geopoint? -181 30)))))

(deftest test-match-literals
  (testing "test1"
    (defun test1
      ([true false] 1)
      ([true true] 2)
      ([false true] 3)
      ([false false] 4))
    (is (= 2 (test1 true true)))
    (is (= 4 (test1 false false)))))

(deftest test-match-vector
  (testing "test3"
    (defun test3
      ([[_ _ 2]] :a0)
      ([[1 1 3]] :a1)
      ([[1 2 3]] :a2))
    (is (= :a2 (test3 [1 2 3])))
    (is (= :a0 (test3 [3 3 2])))
    (is (= :a1 (test3 [1 1 3]))))
  (testing "test2"
    (defun test2
      ([([1] :seq)] :a0)
      ([([1 2] :seq)] :a1)
      ([([1 2 nil nil nil] :seq)] :a2))
    (is (= :a2 (test2 [1 2 nil nil nil])))))

(deftest test-private-macro
  (testing "private macro"
    (defun- test1
      ([_]))
    (is (:private (meta #'test1)))))

(deftest side-effects
  (testing "site-effects"
    (defun test1
      ([1] 1)
      ([x] (println "Square")
         (* 2 x)))
    (with-out-str
      (is (= 4 (test1 2))))
    (is (= "Square") (with-out-str (test1 2)))))

(deftest test-meta
  (testing "meta"
    (defun hello
      "hello world"
      ([name] (str "hello," name))
      ([a b] "unknown."))
    (is (= "hello world" (-> #'hello meta :doc)))
    (is (= '([name] [a b])) (-> #'hello meta :arglists))))

(deftest test-fun
  (testing "fun"
    (is (= 0 ((fun [] 0))))
    (is (= 0 ((fun test [] 0))))
    (is (= 0 ((fun ([] 0) ([a] a) ([a b] b)))))
    (is (= 2 ((fun ([] 0) ([a] a) ([a b] b)) 2)))
    (is (= 1 ((fun ([] 0) ([a] a) ([a b] b)) 1)))
    (is (= 2 ((fun ([] 0) ([a] a) ([a b] b)) 1 2)))))

(deftest test-letfun
  (testing "letfun"
    (letfun [(twice [x]
                    (* x 2))
             (six-times [y]
                        (* (twice y) 3))
             (accum
              ([0 ret] ret)
              ([n ret] (recur (dec n) (+ n ret)))
              ([n] (recur n 0)))]
            (is (= 30 (twice 15)))
            (is (= 90) (six-times 15))
            (is (= 5050 (accum 100))))
    (is (nil? (resolve 'six-times)))
    (is (nil? (resolve 'twice)))
    (is (nil? (resolve 'accum)))))
