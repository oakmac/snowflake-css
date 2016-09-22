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
(def path-lib (js/require "path"))

;;------------------------------------------------------------------------------
;; State
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
;; - handle the case where there is no snowflake.config file
(defn- read-project-config [path]
 (let [config-file (.join path-lib path "snowflake.json")
       project-config (jsonfile->clj config-file)]
   (expand-config project-config)))

(def homedir (.getHomeDirectory fs))
(def projects-file (.join path-lib homedir ".snowflake-projects.json"))

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

;; NOTE: useful for debugging
;; (add-watch projects :log atom-logger)

(defn- load-project-config! [path]
  (let [project-config (read-project-config path)]
    (swap! state update-in [path] merge project-config)))

(defn- load-projects-file! []
  (let [projects-vector (jsonfile->clj projects-file)
        initial-configs (map (fn [x] {:path x}) projects-vector)]
    (reset! state (zipmap projects-vector initial-configs))))

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

;; TODO: this is inefficient and slow, needs to be refactored to be asynchronous
(defn- load-everything! []
  (load-projects-file!)
  (doseq [path (keys @state)]
    (load-project-config! path))
  (doseq [prj (vals @state)]
    (read-flakes-for-project! prj)))

;; NOTE: this architecture needs to be re-thought
;; this is fine for development purposes though

;; reload everything every minute
(def one-minute (* 60 1000))
(js/setInterval load-everything! one-minute)

;; push the state to the client every second
(def one-second 1000)
(js/setInterval
  (fn [] (swap! state identity))
  one-second)

;;------------------------------------------------------------------------------
;; Socket Events
;;------------------------------------------------------------------------------

(def io nil)

(defn- on-socket-disconnect [])
  ;; (js-log "socket connection lost"))

(defn- on-socket-connection [socket]
  ;; (js-log "socket connection established!")
  (.on socket "disconnect" on-socket-disconnect))

;; TODO: switch to using transit.cljs here
(defn- send-state-to-clients [_ _ _ new-state]
  ;; (js-log "sending state to clients...")
  (.emit io "new-state" (pr-str new-state)))

(add-watch state :socket-emit send-state-to-clients)

;;------------------------------------------------------------------------------
;; Server Initialization
;;------------------------------------------------------------------------------

(def app (express))
(def server nil)

(defn -main [& args]
  ;; serve static files out of /public
  (.use app (express-static (.join path-lib js/__dirname "public")))
  ;; start the server
  (set! server (.listen app (:port config)))
  ;; connect socket.io
  (set! io (.listen io-lib server))
  (.on io "connection" on-socket-connection)

  (load-everything!)

  (js-log (str "Snowflake CSS server running on port " (:port config))))

(set! *main-cli-fn* -main)
