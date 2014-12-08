(defproject telepaint-ui "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [jayq "2.5.2"]
                 [com.cognitect/transit-cljs "0.8.194"]]
  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "telepaint-ui"
              :source-paths ["src"]
              :compiler {
                :output-to "telepaint_ui.js"
                :output-dir "out"
                :optimizations :none
                :externs ["externs/jquery.js"]
                :source-map true}}]})
