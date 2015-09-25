(defproject defun "0.2.0"
  :description "A beautiful macro to define clojure functions"
  :url "https://github.com/killme2008/defun"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:1.6 {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/core.match "0.2.1"]
                                  [org.clojure/tools.macro "0.1.2"]]}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/tools.macro "0.1.2"]])
