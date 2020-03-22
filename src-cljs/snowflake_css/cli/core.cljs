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

(defn- get-snowflake-classes-from-file [file]
  ; (timbre/info "Reading" file "…")
  (let [file-contents (.readFileSync fs file "utf8")
        hashes (->> (re-seq possibles-regex file-contents)
                    (map first)
                    (filter snowflake-class?)
                    set)]
    (timbre/info "Found" (count hashes) "snowflake classes in" file)
    hashes))

;; TODO: need to throw an error here if css-content cannot be parsed
(defn- remove-classes-from-css [css-content classes-to-remove]
  (let [root-node (.parse postcss css-content)]
    (.walkRules root-node
      (fn [node]
        (let [selector (oget node "selector")]
          (doseq [c classes-to-remove] ;; <-- FIXME: this is slow; O(n^2)
            (when (str/starts-with? selector (str "." c))
              (.remove node))))))
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

;; TODO: command line options
;; --config-file
;; --eject
;; --scramble
;; --rename
;; --show-orphans
;; --output-file
; (defn- main [& js-args]
;   (timbre/merge-config! {:output-fn format-log-msg})
;   (let [args (js->clj (oget yargs "argv") :keywordize-keys true)
;         config-file (get args :config-file default-config-file)
;         config-file-options (try
;                               (-> (.readFileSync fs config-file)
;                                   js/JSON.parse
;                                   (js->clj :keywordize-keys true))
;                               (catch js/Object e nil))
;         options (if config-file-options
;                   config-file-options
;                   args)
;
;
;         css-files-glob (:css-files options)
;         css-is-file? (.isFileSync fs css-files-glob)
;         templates-path (:template-files options)
;         templates-path-is-file? (.isFileSync fs templates-path)
;
;         ;; TODO: add an "overwrite-css-file" option that only works if they provide
;         ;; a single CSS file as an option
;
;         ;; FIXME: if they pass in --output-css-file and it is the same thing as
;         ;; -css-input then throw a warning
;
;         css-files (if css-is-file?
;                     [css-search]
;                     (glob-sync css-search))
;         templates-files (if templates-path-is-file?
;                           [templates-path]
;                           (glob-sync templates-path))
;
;         _ (timbre/info "Reading CSS and template files for snowflake classes …")
;         css-classes (reduce
;                       (fn [acc file]
;                         (assoc acc file (get-snowflake-classes-from-file file)))
;                       {}
;                       css-files)
;         template-classes (reduce
;                            (fn [acc file]
;                              (assoc acc file (get-snowflake-classes-from-file file)))
;                            {}
;                            templates-files)
;
;         all-css-classes (apply set/union (vals css-classes))
;         all-template-classes (apply set/union (vals template-classes))
;         orphan-css-classes (set/difference all-css-classes all-template-classes)
;         orphan-template-classes (set/difference all-template-classes all-css-classes)
;
;         first-css-file-contents (.readFileSync fs (first css-files) "utf8")]
;
;     (timbre/info "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
;     (timbre/info "Results:")
;     (timbre/info "Found" (count all-css-classes) "snowflake classes in" (count css-classes) "CSS target files")
;     (timbre/info "Found" (count all-template-classes) "snowflake classes in" (count template-classes) "template target files")
;     (when-not (empty? orphan-template-classes)
;       (timbre/warn (count orphan-template-classes) "template classes not found in CSS:" (vec orphan-template-classes)))
;     (when-not (empty? orphan-css-classes)
;       (timbre/warn (count orphan-css-classes) "classes not found in CSS:" (vec orphan-css-classes)))
;
;
;     ;; TODO: log file size before / after here and the number of rules removed
;     (when (:output-css-file args)
;       (let [new-css-file (remove-classes-from-css first-css-file-contents orphan-css-classes)]
;         (.writeFileSync fs (:output-css-file args) new-css-file "utf8")))))

(defmulti run-command!
  (fn [command-type _opts] command-type))

(defmethod run-command! :build
  [_command-type {:keys [css-snowflake-classes
                         all-css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "BUILD COMMAND go go!!")
  (process-exit!))

(defmethod run-command! :list
  [_command-type {:keys [css-snowflake-classes
                         all-css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "LIST COMMAND go go!!")
  (process-exit!))

(defmethod run-command! :rename
  [_command-type {:keys [css-snowflake-classes
                         all-css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "RENAME COMMAND go go!!")
  (process-exit!))

(defmethod run-command! :default
  [command-type _opts]
  (timbre/error "Invalid command-type passed to run-command!" command-type)
  (process-exit! 1))

(defn- command-handler
  "this function runs for every yargs command handler
  parses the JavaScript args and builds snowflake data structures from the filesystem / config
  then calls a corresponding defmethod for the 'command' type"
  [command-type js-args]
  (let [args (js->clj js-args :keywordize-keys true)
        css-files (get-files-from-pattern-arg (:cssFiles args))
        css-snowflake-classes (get-snowflake-classes-from-files css-files)
        all-css-snowflake-classes (apply set/union (vals css-snowflake-classes))
        template-files (get-files-from-pattern-arg (:templateFiles args))
        template-snowflake-classes (get-snowflake-classes-from-files template-files)
        all-template-snowflake-classes (apply set/union (vals template-snowflake-classes))]
    (run-command! command-type {:css-files css-files
                                :css-snowflake-classes css-snowflake-classes
                                :all-css-snowflake-classes all-css-snowflake-classes
                                :template-files template-files
                                :template-snowflake-classes template-snowflake-classes
                                :all-template-snowflake-classes all-template-snowflake-classes})))

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

(def js-build-command
  (js-obj
    "command" "build"
    "describe" (str "Takes a CSS file as input and removes any ruleset that references "
                    "a snowflake class selector that is not found in template files")
    "handler" (partial command-handler :build)))

(def js-rename-command
  (js-obj
    "command" "rename"
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
