(defproject snowflake "0.1.0"
  :description "Snowflake CSS Server and Tooling"
  :url "https://github.com/oakmac/snowflake-css"

  :license {:name "ISC License"
            :url "https://github.com/oakmac/snowflake-css/blob/master/LICENSE.md"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.oakmac/util "2.0.1"]
                 [rum "0.10.5"]]

  :plugins [[lein-cljsbuild "1.1.2"]]

  :source-paths ["src"]

  :clean-targets ["app.js"
                  "public/js/app-dev.js"
                  "public/js/app-prod.js"
                  "target"]

  :cljsbuild
    {:builds
      [{:id "client-dev"
        :source-paths ["cljs-client"]
        :compiler {:optimizations :whitespace
                   :output-to "public/js/app-dev.js"}}

      ;  {:id "client-prod"
      ;   :source-paths ["cljs-client"]
      ;   :compiler {:optimizations :advanced
      ;              :output-to "public/js/app-prod.js"
      ;              :pretty-print false}}

       {:id "server"
        :source-paths ["cljs-server"]
        :compiler {:language-in :ecmascript5
                   :language-out :ecmascript5
                   :optimizations :simple
                   :output-to "app.js"
                   :target :nodejs}}]})
