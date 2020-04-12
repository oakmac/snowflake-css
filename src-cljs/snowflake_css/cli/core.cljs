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
;; FIXME: organize me

(defn format-log-msg [{:keys [instant level msg_]}]
  (let [level-str (-> level name str/upper-case)
        msg (str (force msg_))]
    (str
      "[snowflake-css] "
      (when (= level-str "WARN") "WARNING: ")
      (when (= level-str "FATAL") "ERROR: ")
      (when (= level-str "ERROR") "ERROR: ")
      msg)))

;; -----------------------------------------------------------------------------
;; Node Helpers

(defn process-exit!
  ([]
   (process-exit! 0))
  ([code]
   (js/process.exit code)))

(defn print-to-console! [a-string]
  (js/console.log a-string))

;; -----------------------------------------------------------------------------
;; Filesystem Helpers

(defn file-exists? [filename]
  (.isFileSync fs filename))

(defn read-file-sync! [filename]
  (.readFileSync fs filename "utf8"))

(defn write-file-sync! [filename file-contents]
  (.writeFileSync fs filename file-contents "utf8"))

(defn glob-sync
  "returns a set of unique files from a glob pattern string"
  [pattern]
  (-> (.sync glob pattern)
      js->clj
      set))

(defn get-files-from-pattern-arg
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

;; -----------------------------------------------------------------------------
;; Flake Parsing

(def possibles-regex #"([a-z0-9]+-){1,}([abcdef0-9]){5}[^abcdef0-9]")

(defn- remove-last-char [s]
  (subs s 0 (dec (count s))))

(assert (= (remove-last-char "aaa") "aa"))
(assert (= (remove-last-char "") ""))

(defn get-snowflake-classes-from-str
  "returns a set of snowflake classes from a string"
  [a-string]
  (->> (re-seq possibles-regex a-string)
       (map first)
       (map remove-last-char)
       (filter snowflake-class?)
       set))

(assert (get-snowflake-classes-from-str "fizzle-44ebc.") #{"fizzle-44ebc"})
(assert (get-snowflake-classes-from-str ".fizzle-44ebc a") #{"fizzle-44ebc"})
(assert (get-snowflake-classes-from-str ".fizzle-44ebc a, .foo-56bec") #{"fizzle-44ebc" "foo-56bec"})
(assert (get-snowflake-classes-from-str "<div class=\"fizzle-44ebc\">") #{"fizzle-44ebc"})
(assert (get-snowflake-classes-from-str " e4d2ad4f-f4f3-420b-ba95-d2b869fc9a6d ") #{})

;; -----------------------------------------------------------------------------
;; Process CSS File

;; TODO: need to throw an error here if css-content cannot be parsed
(defn remove-flakes-from-css [css-content flakes-to-keep-around]
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

;; -----------------------------------------------------------------------------
;; Get Flakes from Files

(defn- file->flakes
  "Reads a file and returns a set of the snowflake classes found in it"
  ([file]
   (file->flakes file false))
  ([file log?]
   (if-not (file-exists? file)
     (do
       (timbre/warn "Cannot read file:" file)
       #{})
     (let [file-contents (read-file-sync! file)
           flakes (get-snowflake-classes-from-str file-contents)]
       (when log?
         (timbre/info "Found" (count flakes) "flakes in" file))
       flakes))))

(defn- files->flakes-map
  "returns a map of filename --> snowflake classes"
  ([files]
   (files->flakes-map files false))
  ([files log?]
   (reduce
     (fn [acc file]
       (assoc acc file (file->flakes file log?)))
     {}
     files)))

;; -----------------------------------------------------------------------------
;; Command defmethods

(defn- valid-arg?
  "Is x a possible CLI argument?"
  [x]
  (and (string? x)
       (not (str/blank? x))))

(assert (valid-arg? "foo"))
(assert (valid-arg? "foo/bar"))
(assert (not (valid-arg? "")))
(assert (not (valid-arg? nil)))

(defn prune!
  [js-args]
  (let [args (js->clj js-args :keywordize-keys true)
        {:keys [inputCSSFile outputCSSFile templateFiles]} args]
    ;; config validation
    (when (or (not inputCSSFile) (not templateFiles))
      (timbre/fatal "prune command needs both \"inputCSSFile\" and \"templateFiles\" config options")
      (timbre/fatal "Please run \"npx snowflake init\" to assist in creating a snowflake-css.json config file")
      (timbre/fatal "Goodbye!")
      (process-exit! 1))
    ;; inputCSSFile should exist
    (when-not (file-exists? inputCSSFile)
      (timbre/fatal "prune command input error: " inputCSSFile "is not a valid file")
      (timbre/fatal "Goodbye!")
      (process-exit! 1))
    ;; FIXME: we could do validation of templateFiles config value here
    (let [write-output-to-file? (string? outputCSSFile)
          log-stuff? write-output-to-file?
          input-css-contents (read-file-sync! inputCSSFile)
          input-css-flakes (get-snowflake-classes-from-str input-css-contents)
          _ (when log-stuff?
              (timbre/info "Found" (count input-css-flakes) "flakes in" inputCSSFile))
          template-files (get-files-from-pattern-arg templateFiles)
          template-files->flakes (files->flakes-map template-files log-stuff?)
          template-flakes (apply set/union (vals template-files->flakes))
          output-css (remove-flakes-from-css input-css-contents template-flakes)
          output-css-flakes (get-snowflake-classes-from-str output-css)
          output-flake-count (count output-css-flakes)]
      (if write-output-to-file?
        (do
          (timbre/info "Total input flakes:" (count input-css-flakes))
          (timbre/info "Total output flakes:" (count template-flakes))
          (write-file-sync! outputCSSFile output-css)
          (timbre/info "Wrote" outputCSSFile "with" output-flake-count "flakes")
          (when-not (= (count input-css-flakes) (count output-css-flakes))
            (timbre/info "Input flakes not found in" outputCSSFile ":" (set/difference input-css-flakes output-css-flakes)))
          (when-not (= (count template-flakes) (count output-css-flakes))
            (timbre/info "Template flakes not found in" outputCSSFile ":" (set/difference template-flakes output-css-flakes))))
          ; (timbre/info "Run \"npx snowflake-css report\" to see a report of flake counts"))
        (print-to-console! output-css))
      (process-exit!))))

(defmulti run-command!
  (fn [command-type _opts _args] command-type))

(defmethod run-command! :prune
  [_command-type
   {:keys [input-css-contents
           input-css-flakes
           template-flakes]}
   {:keys [outputCSSFile]}]
  (let [input-css-minus-orphan-flakes (remove-flakes-from-css input-css-contents template-flakes)
        write-to-output-file? (string? outputCSSFile)]
    (cond
      write-to-output-file?
      (do
        (write-file-sync! outputCSSFile input-css-minus-orphan-flakes)
        ;; FIXME: log what happened here
        (process-exit!))

      :else (print-to-console! input-css-minus-orphan-flakes))
    (process-exit!)))

(defmethod run-command! :list
  [_command-type {:keys [css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "FIXME: write the list command")
  (process-exit!))

(defmethod run-command! :rename
  [_command-type {:keys [css-snowflake-classes
                         template-snowflake-classes
                         all-template-snowflake-classes]}]
  (timbre/info "FIXME: write the rename command")
  (process-exit!))

(defmethod run-command! :default
  [command-type _opts _args]
  (timbre/error "Invalid command-type passed to run-command!" command-type)
  (process-exit! 1))

;; FIXME: input validation
;; - inputCSSFile must exist
;; - inputCSSFile must be a syntax-valid CSS File

(defn- command-handler
  "this function runs for every yargs command handler
  parses the JavaScript args and builds snowflake data structures from the filesystem / config
  then calls a corresponding defmethod for the 'command' type"
  [command-type js-args]
  (let [args (js->clj js-args :keywordize-keys true)
        input-css-contents (read-file-sync! (:inputCSSFile args))
        input-css-flakes (get-snowflake-classes-from-str input-css-contents)
        template-files (get-files-from-pattern-arg (:templateFiles args))
        template-files->flakes (files->flakes-map template-files)
        template-flakes (apply set/union (vals template-files->flakes))]
    (run-command!
      command-type
      {:input-css-flakes input-css-flakes
       :input-css-contents input-css-contents
       :template-flakes template-flakes}
      args)))

;; -----------------------------------------------------------------------------
;; CLI Commands

;; What other commands here?
;; - prune    "Build a CSS file using only non-orphan snowflake classes"
;; - eject    "Remove all snowflake hashes"
;; - init     "Create a snowflake-css.json config file"
;; - orphans  "Show a list of all orphaned snowflake classes"
;; - report   "Print a report of how many classes are recognized in which files"
;; - scramble "Scramble snowflake hashes"

;; FIXME: snowflake.js banana should show "banana is an unrecognized command"

(def js-prune-command
  (js-obj
    "command" "prune"
    "describe" (str "Takes a CSS file as input and removes any ruleset that references "
                    "a snowflake class selector not found in template files")
    "handler" prune!))

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

(def default-config-file "./snowflake-css.json")

(defn- main [& js-args]
  (timbre/merge-config! {:output-fn format-log-msg})
  (doto yargs
    (.config)
    (.default "config" default-config-file)
    (.command js-prune-command)
    ; (.command js-rename-command)
    ; (.command js-list-command)
    (.demandCommand) ;; show them --help if they do not pass in a command
    (.help)
    (.-argv)))
