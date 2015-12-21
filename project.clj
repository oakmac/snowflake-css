(defproject snowflake "0.1.0"
  :description "Snowflake CSS Server and Tooling"
  :url "https://github.com/oakmac/snowflake-css"

  :license {:name "ISC License"
            :url "https://github.com/oakmac/snowflake-css/blob/master/LICENSE.md"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [rum "0.5.0"]]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :source-paths ["src"]

  :clean-targets ["app.js"
                  "public/js/app.js"
                  "public/js/app.min.js"
                  "target"]

  :cljsbuild
    {:builds
      [{:id "client-dev"
        :source-paths ["cljs-client"]
        :compiler {:optimizations :whitespace
                   :output-to "public/js/app.js"}}

       {:id "client-min"
        :source-paths ["cljs-client"]
        :compiler {:optimizations :advanced
                   :output-to "public/js/app.min.js"
                   :pretty-print false}}

       {:id "server"
        :source-paths ["cljs-server"]
        :compiler {:language-in :ecmascript5
                   :language-out :ecmascript5
                   :optimizations :simple
                   :output-to "app.js"
                   :target :nodejs}}]})
