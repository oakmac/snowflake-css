(ns snowflake-server.core
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.string :refer [split split-lines]]
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
(def glob (js/require "glob"))

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

(def snowflake-regex #"([a-z0-9]+-){1,}([abcdef0-9]){5}")

(defn- snowflake-class?
  "Is string s a snowflake class?"
  [s]
  (let [the-hash (last (split s "-"))]
    (and (.test snowflake-regex s) ;; must match this regex
         (.test #"\d" s)           ;; must have some numbers
         (.test #"[a-z]" s)        ;; must have some alpha characters
         (.test #"\d" the-hash)    ;; hash must contain at least one number
         (.test #"[a-z]" the-hash)))) ;; has must contain at least one alpha character

;; NOTE: this function could probably be cleaner
(defn- get-classes-from-file [file]
  (let [file-contents (.readFileSync fs file "utf8")
        lines (split-lines file-contents)
        classes (atom [])]
    (dorun
      (map-indexed
        (fn [idx line]
          (let [classes1 (re-seq snowflake-regex line)
                classes2 (map first classes1)
                classes3 (filter snowflake-class? classes2)]
            (doseq [c classes3]
              (swap! classes conj {:class c
                                   :file file
                                   :line-no (inc idx)}))))
        lines))
    (deref classes)))

(defn- load-project-configs! []
  (doseq [p (keys @projects)]
    (let [project-config (read-project-config p)]
      (swap! projects assoc p project-config))))

(defn- load-projects-file! []
  (let [projects-vector (jsonfile->clj projects-file)
        initial-configs (map (fn [x] {:path x}) projects-vector)]
    (reset! projects (zipmap projects-vector initial-configs))))

;; NOTE: useful for debugging
;; (add-watch projects :log atom-logger)

(load-projects-file!)
(load-project-configs!)

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
