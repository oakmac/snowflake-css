(defproject snowflake "0.1.0"

  :description "Snowflake CSS server"
  :url "https://github.com/oakmac/snowflake-css"
  :license {
    :name "MIT License"
    :url "https://github.com/oakmac/snowflake-css/blob/master/LICENSE"
    :distribution :repo}

  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/clojurescript "0.0-3126"]
    [org.clojure/core.async "0.1.346.0-17112a-alpha"]
    [hiccups "0.3.0"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :source-paths ["src"]

  :clean-targets ["app.js" "out"]

  :cljsbuild {
    :builds {
     :server {
      :source-paths ["src-cljs"]
      :compiler {
        :language-in :ecmascript5
        :language-out :ecmascript5
        :target :nodejs
        :output-to "app.js"
        :optimizations :simple }}}})
