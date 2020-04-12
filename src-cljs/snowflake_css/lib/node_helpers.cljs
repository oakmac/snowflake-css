(ns snowflake-css.lib.node-helpers)

(defn process-exit!
  ([]
   (process-exit! 0))
  ([code]
   (js/process.exit code)))

(defn print-to-console! [a-string]
  (js/console.log a-string))
