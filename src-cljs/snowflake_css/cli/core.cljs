(ns snowflake-css.cli.core
  (:require
    ["yargs" :as yargs]
    [clojure.set :as set]
    [oops.core :refer [oget]]
    [snowflake-css.lib.filesystem-helpers :as fs]
    [snowflake-css.lib.flake-parsing :refer [files->flakes-map string->flakes remove-flakes-from-css-string]]
    [snowflake-css.lib.logging :as logging]
    [snowflake-css.lib.node-helpers :refer [print-to-console! process-exit!]]
    [snowflake-css.lib.predicates :refer [snowflake-class?]]
    [taoensso.timbre :as timbre]))

;; -----------------------------------------------------------------------------
;; Prune Command

(defn prune!
  [js-args]
  (let [args (js->clj js-args :keywordize-keys true)
        {:keys [inputCSSFile outputCSSFile templateFiles]} args]
    ;; config validation
    (when (or (not inputCSSFile) (not templateFiles))
      (timbre/fatal "The prune command needs both \"inputCSSFile\" and \"templateFiles\" config options.")
      (timbre/fatal "Please run \"npx snowflake init\" to create a snowflake-css.json config file,")
      (timbre/fatal "or pass in those values directly with --inputCSSFile and --templateFiles CLI flags.")
      (timbre/fatal "Type \"npx snowflake help\" to learn more. Goodbye!")
      (process-exit! 1))
    ;; inputCSSFile should exist
    (when-not (fs/file-exists? inputCSSFile)
      (timbre/fatal "Config value \"inputCSSFile\" is not a valid file:" inputCSSFile)
      (timbre/fatal "Please check your snowflake-css.json config file or CLI arguments. Goodbye!")
      (process-exit! 1))
    ;; FIXME: templateFiles validation here
    (let [write-output-to-file? (string? outputCSSFile)
          log-stuff? write-output-to-file?
          input-css-contents (fs/read-file-sync! inputCSSFile)
          input-css-flakes (string->flakes input-css-contents)
          _ (when log-stuff?
              (timbre/info "Found" (count input-css-flakes) "flakes in" inputCSSFile))
          template-files (fs/glob-pattern->files templateFiles)
          template-files->flakes (files->flakes-map template-files log-stuff?)
          template-flakes (apply set/union (vals template-files->flakes))
          output-css (remove-flakes-from-css-string input-css-contents template-flakes)
          output-css-flakes (string->flakes output-css)
          output-flake-count (count output-css-flakes)]
      (if write-output-to-file?
        (do
          (timbre/info "Total input flakes:" (count input-css-flakes))
          (timbre/info "Total output flakes:" (count template-flakes))
          (fs/write-file-sync! outputCSSFile output-css)
          (timbre/info "Wrote" outputCSSFile "with" output-flake-count "flakes")
          (when-not (= (count input-css-flakes) (count output-css-flakes))
            (timbre/info "Input flakes not found in" outputCSSFile ":" (set/difference input-css-flakes output-css-flakes)))
          (when-not (= (count template-flakes) (count output-css-flakes))
            (timbre/info "Template flakes not found in" outputCSSFile ":" (set/difference template-flakes output-css-flakes))))
        ;; else print the pruned CSS output to console directly (for piping)
        (print-to-console! output-css))
      (process-exit!))))

(defmulti run-command!
  (fn [command-type _opts _args] command-type))

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
        input-css-contents (fs/read-file-sync! (:inputCSSFile args))
        input-css-flakes (string->flakes input-css-contents)
        template-files (fs/glob-pattern->files (:templateFiles args))
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
  (timbre/merge-config! {:output-fn logging/format-log-msg})
  (doto yargs
    (.config)
    (.default "config" default-config-file)
    (.command js-prune-command)
    ; (.command js-rename-command)
    ; (.command js-list-command)
    (.demandCommand) ;; show them --help if they do not pass in a command
    (.help)
    (.-argv)))
