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
    [hiccups "0.3.0"]
    [quiescent "0.1.4"]
    [sablono "0.3.1"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :source-paths ["src"]

  :clean-targets [
    "app.js"
    "public/js/main.js"
    "public/js/main.min.js"
    "out"]

  :cljsbuild {
    :builds {
      :client-dev {
        :source-paths ["cljs-client"]
        :compiler {
          :output-to "public/js/main.js"
          :optimizations :whitespace }}

      :client-prod {
        :source-paths ["cljs-client"]
        :compiler {
          :output-to "public/js/main.min.js"
          :optimizations :advanced }}

      :server {
        :source-paths ["cljs-server"]
        :compiler {
          :language-in :ecmascript5
          :language-out :ecmascript5
          :target :nodejs
          :output-to "app.js"
          :optimizations :simple }}}})
