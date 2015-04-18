(ns snowflake.core
  (:require
    [snowflake.util :refer [js-log log]]))

(defn- foo []
  (js-log "foo")
  )

(defn -main [& args]
  ;;(js-log "Hello world!")
  (js/setInterval foo 1000)
  )

(set! *main-cli-fn* -main)
