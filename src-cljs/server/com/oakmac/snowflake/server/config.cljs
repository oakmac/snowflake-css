(ns com.oakmac.snowflake.server.config
  (:require
    [clojure.walk :refer [keywordize-keys]]))

;;------------------------------------------------------------------------------
;; Config
;;------------------------------------------------------------------------------

(def default-config
  {:in-development? true
   :port 5004})

(def config
  (try
    (->> (js/require "./config.json")
         js->clj
         keywordize-keys
         (merge default-config))
    (catch :default e
      default-config)))
