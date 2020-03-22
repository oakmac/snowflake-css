(ns snowflake-css.cli.core
  (:require
    ["fs-plus" :as fs]
    ["glob" :as glob]
    ["postcss" :as postcss]
    ["yargs" :as yargs]
    [clojure.set :as set]
    [clojure.string :as str]
    [oops.core :refer [oget]]
    [snowflake-css.lib.predicates :refer [snowflake-class?]]
    [taoensso.timbre :as timbre]))

;; -----------------------------------------------------------------------------
;; TODO: move these functions into namespaces

(defn process-exit!
  ([]
   (process-exit! 0))
  ([code]
   (js/process.exit code)))

(defn format-log-msg [{:keys [instant level msg_]}]
  (let [level-str (-> level name str/upper-case)
        msg (str (force msg_))]
    (str
      "[snowflake-css] "
      (when (= level-str "WARN") "WARNING: ")
      msg)))

(def possibles-regex #"([a-zA-Z0-9]+-){1,}([abcdef0-9]){5}")

(defn- read-file-sync! [filename]
  (.readFileSync fs filename "utf8"))

(defn- write-file-sync! [filename file-contents]
  (.writeFileSync fs filename file-contents "utf8"))

(defn- get-snowflake-classes-from-str
  "returns a set of snowflake classes from a string"
  [a-string]
  (->> (re-seq possibles-regex a-string)
       (map first)
       (filter snowflake-class?)
       set))

(assert (get-snowflake-classes-from-str "fizzle-44ebc") #{"fizzle-44ebc"})
(assert (get-snowflake-classes-from-str ".fizzle-44ebc a") #{"fizzle-44ebc"})
(assert (get-snowflake-classes-from-str ".fizzle-44ebc a, .foo-56bec") #{"fizzle-44ebc" "foo-56bec"})
(assert (get-snowflake-classes-from-str "<div class=\"fizzle-44ebc\">") #{"fizzle-44ebc"})

(defn- get-snowflake-classes-from-file [file]
  ; (timbre/info "Reading" file "…")
  (let [file-contents (read-file-sync! file)
        hashes (->> (re-seq possibles-regex file-contents)
                    (map first)
                    (filter snowflake-class?)
                    set)]
    (timbre/info "Found" (count hashes) "snowflake classes in" file)
    hashes))

;; TODO: need to throw an error here if css-content cannot be parsed
(defn- remove-flakes-from-css [css-content flakes-to-keep-around]
  (let [root-node (.parse postcss css-content)]
    (.walkRules root-node
      (fn [js-node]
        (let [selector (oget js-node "selector")
              flakes-in-selector (get-snowflake-classes-from-str selector)]
          ;; 1) only look at rulesets that contain snowflake classes
          ;; 2) remove any ruleset that does not exist in flakes-to-keep-around
          (when (and (not (empty? flakes-in-selector))
                     (empty? (set/intersection flakes-in-selector flakes-to-keep-around)))
            (.remove js-node)))))
    (oget (.toResult root-node) "css")))

(defn- glob-sync
  "returns a set of unique files from a glob pattern string"
  [pattern]
  (-> (.sync glob pattern)
      js->clj
      set))

(defn- get-files-from-pattern-arg
  "return a sorted list of files from a glob pattern config argument
   the glob pattern can either be a string or a list / vector"
  [pattern-arg]
  (let [list-of-glob-patterns (if (string? pattern-arg) [pattern-arg] pattern-arg)
        files (reduce
                (fn [all-files pattern]
                  (set/union all-files (glob-sync pattern)))
                #{}
                list-of-glob-patterns)]
    (-> files
        (into [])
        sort)))

(defn- get-snowflake-classes-from-files
  "returns a map of filename --> snowflake classes"
  [files]
  (reduce
    (fn [acc file]
      (assoc acc file (get-snowflake-classes-from-file file)))
    {}
    files))

;; -----------------------------------------------------------------------------
;; Main Entry Point

(def default-config-file "./snowflake-css.json")

(defmulti run-command!
  (fn [command-type _opts _args] command-type))

(defmethod run-command! :build
  [_command-type
   {:keys [css-snowflake-classes
           template-snowflake-classes
           input-css-contents]}
   {:keys [outputCSSFile]}]
  (let [input-minus-null-flakes (remove-flakes-from-css input-css-contents template-snowflake-classes)]
    (write-file-sync! outputCSSFile input-minus-null-flakes)
    (process-exit!)))

(defmethod run-command! :list
  [_command-type {:keys [css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "LIST COMMAND go go!!")
  (process-exit!))

(defmethod run-command! :rename
  [_command-type {:keys [css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "RENAME COMMAND go go!!")
  (process-exit!))

(defmethod run-command! :default
  [command-type _opts _args]
  (timbre/error "Invalid command-type passed to run-command!" command-type)
  (process-exit! 1))

;; TODO: input validation
;; - inputCSSFile must exist
;; - inputCSSFile must be a syntax-valid CSS File

(defn- command-handler
  "this function runs for every yargs command handler
  parses the JavaScript args and builds snowflake data structures from the filesystem / config
  then calls a corresponding defmethod for the 'command' type"
  [command-type js-args]
  (let [args (js->clj js-args :keywordize-keys true)
        input-css-contents (read-file-sync! (:inputCSSFile args))
        css-snowflake-classes (get-snowflake-classes-from-str input-css-contents)
        template-files (get-files-from-pattern-arg (:templateFiles args))
        template-files->snowflake-classes (get-snowflake-classes-from-files template-files)
        template-snowflake-classes (apply set/union (vals template-files->snowflake-classes))]
    (run-command!
      command-type
      {:css-snowflake-classes css-snowflake-classes
       :input-css-contents input-css-contents
       :template-snowflake-classes template-snowflake-classes}
      args)))

;; -----------------------------------------------------------------------------
;; CLI Commands

;; What other commands here?
;; - build    "Build a CSS file using only non-orphan snowflake classes"
;; - config
;; - eject    "Remove all snowflake hashes"
;; - init     "Create a snowflake-css.json config file"
;; - orphans  "Show a list of all orphaned snowflake classes"
;; - report   "Print a report of how many classes recognized in what files"
;; - scramble "Scramble snowflake hashes"

;; "default" command (ie: no argument passed) should be "help" (show the options)

(def js-build-command
  (js-obj
    "command" "build"
    "describe" (str "Takes a CSS file as input and removes any ruleset that references "
                    "a snowflake class selector that is not found in template files")
    "handler" (partial command-handler :build)))

(def js-rename-command
  (js-obj
    "command" "rename <from> <to>"
    "describe" "Rename a snowflake class"
    "handler" (partial command-handler :rename)))

;; what other names for this? list, show, print
(def js-list-command
  (js-obj
    "command" "list"
    "describe" "List all known snowflake classes"
    "handler" (partial command-handler :rename)))

;; -----------------------------------------------------------------------------
;; Main Entry Point

(defn- main [& js-args]
  (timbre/merge-config! {:output-fn format-log-msg})
  (doto yargs
    (.config)
    (.default "config" "./snowflake-css.json")
    (.command js-build-command)
    (.command js-rename-command)
    (.command js-list-command)
    (.help)
    (.-argv)))
