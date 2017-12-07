(ns com.oakmac.snowflake.client.util)

;;------------------------------------------------------------------------------
;; Logging
;;------------------------------------------------------------------------------

(defn js-log
  "Logs a JavaScript thing."
  [js-thing]
  (js/console.log js-thing))


(defn log
  "Logs a Clojure thing."
  [clj-thing]
  (js-log (pr-str clj-thing)))


;;------------------------------------------------------------------------------
;; DOM
;;------------------------------------------------------------------------------

(defn by-id [id]
  (.getElementById js/document id))
