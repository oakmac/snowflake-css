(ns snowflake-server.core
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.walk :refer [keywordize-keys]]
    [oakmac.util :refer [atom-logger js-log log]]
    [snowflake-server.config :refer [config]]))

(nodejs/enable-util-print!)

;;------------------------------------------------------------------------------
;; Node Libraries
;;------------------------------------------------------------------------------

(def express (js/require "express"))
(def express-static (aget express "static"))
(def fs (js/require "fs-plus"))

;;------------------------------------------------------------------------------
;; Util
;;------------------------------------------------------------------------------

;; TODO: wrap this in a try/catch
(defn- jsonfile->clj [f]
  (->> (.readFileSync fs f "utf8")
       js/JSON.parse
       js->clj
       keywordize-keys))

;;------------------------------------------------------------------------------
;; Projects
;;------------------------------------------------------------------------------

;; NOTE: this function would be a good candidate for some tests
(defn- expand-config
  "Expand a shorthand config to it's full form."
  [config]
  (loop [c config]
    (cond
      (string? (:css-files c))
      (recur (assoc c :css-files [(:css-files c)]))

      (vector? (:css-files c))
      (recur (assoc c :css-files {:include (:css-files c)}))

      (string? (get-in c [:css-files :include]))
      (recur (assoc-in c [:css-files :include] [(get-in c [:css-files :include])]))

      (string? (get-in c [:css-files :exclude]))
      (recur (assoc-in c [:css-files :exclude] [(get-in c [:css-files :exclude])]))

      (string? (:application-files c))
      (recur (assoc c :application-files [(:application-files c)]))

      (vector? (:application-files c))
      (recur (assoc c :application-files {:include (:application-files c)}))

      (string? (get-in c [:application-files :include]))
      (recur (assoc-in c [:application-files :include] [(get-in c [:application-files :include])]))

      (string? (get-in c [:application-files :exclude]))
      (recur (assoc-in c [:application-files :exclude] [(get-in c [:application-files :exclude])]))

      :else
      c)))

;; TODO:
;; - change this to use the path module
;; - handle the case where there is no snowflake.config file
(defn- read-project-config [path]
 (let [config-file (str path "snowflake.json")
       project-config (jsonfile->clj config-file)]
   (expand-config project-config)))

(def projects-file (str (.getHomeDirectory fs) "/.snowflake-projects.json"))

(def projects (atom {}))

;; TODO: this could be cleaned up
(defn- load-projects-file! []
  (let [projects-vector (jsonfile->clj projects-file)]
    (reset! projects (zipmap projects-vector (repeat (count projects-vector) {}))))
  ;; load each project's config
  (doseq [p (keys @projects)]
    (let [project-config (read-project-config p)]
      (swap! projects assoc p project-config))))

(add-watch projects :log atom-logger)

(load-projects-file!)

;;------------------------------------------------------------------------------
;; Server Initialization
;;------------------------------------------------------------------------------

(def app (express))

(defn -main [& args]
  (doto app
    ;; middleware
    (.use (express-static (str js/__dirname "/public")))

    (.listen (:port config)))
  (js-log (str "Snowflake server running on port " (:port config))))

(set! *main-cli-fn* -main)
