(ns snowflake-css.lib.logging
  (:require
    [clojure.string :as str]))

;; TODO: use chalk library here for some coloring?
;; https://github.com/chalk/chalk

(defn format-log-msg [{:keys [instant level msg_]}]
  (let [level-str (-> level name str/upper-case)
        msg (str (force msg_))]
    (str
      "[snowflake-css] "
      (when (= level-str "WARN") "WARNING: ")
      (when (= level-str "FATAL") "ERROR: ")
      (when (= level-str "ERROR") "ERROR: ")
      msg)))
