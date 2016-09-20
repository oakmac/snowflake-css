(ns snowflake-server.core
  (:require
    [cljs.nodejs :as nodejs]
    [clojure.string :refer [split-lines]]
    [clojure.set :refer [difference union]]
    [clojure.walk :refer [keywordize-keys]]
    [oakmac.util :refer [atom-logger js-log log]]
    [snowflake-server.config :refer [config]]
    [snowflake-server.util :refer [jsonfile->clj
                                   read-flakes-from-files]]))

(nodejs/enable-util-print!)

;;------------------------------------------------------------------------------
;; Node Libraries
;;------------------------------------------------------------------------------

(def express (js/require "express"))
(def express-static (aget express "static"))
(def fs (js/require "fs-plus"))
(def glob (js/require "glob"))
(def http (js/require "http"))
(def io-lib (js/require "socket.io"))

;;------------------------------------------------------------------------------
;; Projects
;;------------------------------------------------------------------------------

(defn- expand-config
  "Expand a shorthand config to it's full form."
  [config]
  (loop [c config]
    (cond
      (string? (:app-files c))
      (recur (assoc c :app-files [(:app-files c)]))

      (string? (:app-files-exclude c))
      (recur (assoc c :app-files-exclude [(:app-files-exclude c)]))

      (string? (:css-files c))
      (recur (assoc c :css-files [(:css-files c)]))

      (string? (:css-files-exclude c))
      (recur (assoc c :css-files-exclude [(:css-files-exclude c)]))

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

(def example-state
  {"/home/user1/project1/"
    {:path "same as the key"
     :name "Project 1" ;; project display name
     :app-files ["src-cljs/**"] ;; vector of glob patterns for the application files
     :app-files-exclude ""
     :css-files "less/*.less" ;; vector of glob patterns for the CSS files
     :css-files-exclude ""
     :css-flakes [] ;; vector of CSS flakes
     :app-flakes []}}) ;; vector of app flakes}})

(def state (atom {}))

(defn- load-project-configs! []
  (doseq [path (keys @state)]
    (let [project-config (read-project-config path)]
      (swap! state update-in [path] merge project-config))))

(defn- load-projects-file! []
  (let [projects-vector (jsonfile->clj projects-file)
        initial-configs (map (fn [x] {:path x}) projects-vector)]
    (reset! state (zipmap projects-vector initial-configs))))

;; NOTE: useful for debugging
;; (add-watch projects :log atom-logger)

(load-projects-file!)
(load-project-configs!)

(defn- glob->files
  "Given a cwd and a glob pattern, returns a set of the matched files."
  [cwd pattern]
  (let [glob-options (js-obj "cwd" cwd
                             "nodir" true
                             "realpath" true)]
    (set (.sync glob pattern glob-options))))

(defn- globs->files
  "Given a collection of glob patterns, return a set of the matched files."
  [cwd patterns]
  (reduce
    (fn [files pattern]
      (union files (glob->files cwd pattern)))
    #{}
    patterns))

(defn- read-flakes-for-project! [prj]
  (let [project-path (:path prj)
        app-files (globs->files project-path (:app-files prj))
        exclude-app-files (globs->files project-path (:app-files-exclude prj []))
        app-files (difference app-files exclude-app-files)
        app-flakes (read-flakes-from-files app-files)
        css-files (globs->files project-path (:css-files prj))
        exclude-css-files (globs->files project-path (:css-files-exclude prj []))
        css-files (difference css-files exclude-css-files)
        css-flakes (read-flakes-from-files css-files)]
    (swap! state update-in [project-path] assoc
      :app-flakes app-flakes
      :css-flakes css-flakes)))

; (read-flakes-for-project! (first (vals @state)))
;
; (let [x (:app-flakes (first (vals @state)))
;       y (:css-flakes (first (vals @state)))]
;   (log (str "app flakes: " (count x)))
;   (log (str "css flakes: " (count y))))

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(defn- on-socket-disconnect []
  (js-log "socket connection lost"))

(defn- on-socket-connection [socket]
  (js-log "socket connection established!")
  (.on socket "disconnect" on-socket-disconnect))

;;------------------------------------------------------------------------------
;; Server Initialization
;;------------------------------------------------------------------------------

(def app (express))
(def server nil)
(def io nil)

(defn -main [& args]
  ;; serve static files out of /public
  (.use app (express-static (str js/__dirname "/public")))
  ;; start the server
  (set! server (.listen app (:port config)))
  ;; connect socket.io
  (set! io (.listen io-lib server))
  (.on io "connection" on-socket-connection)

  (js-log (str "Snowflake CSS server running on port " (:port config))))

(set! *main-cli-fn* -main)
