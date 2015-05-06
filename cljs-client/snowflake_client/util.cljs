(ns snowflake-client.util)

;;------------------------------------------------------------------------------
;; Misc Utilties
;;------------------------------------------------------------------------------

(defn js-log
  "Logs a JavaScript thing."
  [js-thing]
  (js/console.log js-thing))

(defn log
  "Logs a Clojure thing."
  [clj-thing]
  (js-log (pr-str clj-thing)))
