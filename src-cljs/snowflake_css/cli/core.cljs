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


(defn get-flakes-data
  "Returns a map of flakes information about the project."
  [{:keys [inputCSSFile outputCSSFile templateFiles] :as config}]
  (let [input-css (fs/read-file-sync! inputCSSFile)
        input-flakes (string->flakes input-css)
        template-files (fs/glob-pattern->files templateFiles)
        template-files->flakes (files->flakes-map template-files)
        template-flakes (apply set/union (vals template-files->flakes))
        output-css (remove-flakes-from-css-string input-css template-flakes)
        output-flakes (string->flakes output-css)
        lonely-input-flakes (set/difference input-flakes output-flakes)
        lonely-template-flakes (set/difference template-flakes output-flakes)]
    {:input-css input-css
     :input-flakes input-flakes
     :lonely-input-flakes lonely-input-flakes
     :lonely-template-flakes lonely-template-flakes
     :output-css output-css
     :output-flakes output-flakes
     :template-files->flakes template-files->flakes
     :template-flakes template-flakes}))


(defn- verify-config!
  "Verify that config information exists and looks correct.
  NOTE: will kill the process with code 1 if detects an error"
  [{:keys [inputCSSFile outputCSSFile templateFiles] :as config} command-name]
  ;; config validation
  (when (or (not inputCSSFile) (not templateFiles))
    (timbre/fatal "The \"" command-name "\" command requires both \"inputCSSFile\" and \"templateFiles\" config options.")
    (timbre/fatal "Please run \"npx snowflake init\" to create a snowflake-css.json config file,")
    (timbre/fatal "or pass in those values directly with --inputCSSFile and --templateFiles CLI flags.")
    (timbre/fatal "Type \"npx snowflake help\" to learn more. Goodbye!")
    (process-exit! 1))
  ;; inputCSSFile should exist
  (when-not (fs/file-exists? inputCSSFile)
    (timbre/fatal "Config value \"inputCSSFile\" is not a valid file:" inputCSSFile)
    (timbre/fatal "Please check your snowflake-css.json config file or CLI arguments. Goodbye!")
    (process-exit! 1))
  ;; TODO: warn here if inputCSSFile is not parse-able by postcss
  ;; TODO: check templateFiles format here
  true)


(defn- flake-map->report-js-obj
  "Convert flake info map to a JavaScript Object"
  [{:keys [input-flakes lonely-input-flakes output-flakes lonely-output-flakes
           template-flakes lonely-template-flakes]}]
  (js-obj
    "inputFlakes" (clj->js input-flakes)
    "inputFlakesCount" (count input-flakes)
    "lonelyInputFlakes" (clj->js lonely-input-flakes)
    "lonelyInputFlakesCount" (count lonely-input-flakes)
    "lonelyTemplateFlakes" (clj->js lonely-template-flakes)
    "lonelyTemplateFlakesCount" (count lonely-template-flakes)
    "outputFlakes" (clj->js output-flakes)
    "outputFlakesCount" (count output-flakes)
    "templateFlakes" (clj->js template-flakes)
    "templateFlakesCount" (count template-flakes)))


(defn report!
  [js-args]
  (let [config (js->clj js-args :keywordize-keys true)]
    (verify-config! config "report")
    (-> (get-flakes-data config)
        flake-map->report-js-obj
        (js/JSON.stringify nil 2)
        js/console.log)
    (process-exit!)))


(defn prune!
  [js-args]
  (let [config (js->clj js-args :keywordize-keys true)]
    (verify-config! config "prune")
    (let [{:keys [inputCSSFile outputCSSFile templateFiles]} config
          {:keys [input-flakes
                  output-css
                  output-flakes
                  template-flakes]} (get-flakes-data config)
          input-flake-count (count input-flakes)
          output-flake-count (count output-flakes)
          write-output-to-file? (string? outputCSSFile)]
      (if write-output-to-file?
        (do
          (timbre/info "CSS input flake count:" (count input-flakes))
          (timbre/info "Template flake count:" (count template-flakes))
          (fs/write-file-sync! outputCSSFile output-css)
          (timbre/info "Wrote output file" outputCSSFile "with" output-flake-count "flakes")
          (when-not (= input-flake-count output-flake-count)
            (timbre/info "Input flakes not found in" outputCSSFile ":" (set/difference input-flakes output-flakes)))
          (when-not (= (count template-flakes) output-flake-count)
            (timbre/info "Template flakes not found in" outputCSSFile ":" (set/difference template-flakes output-flakes))))
        ;; else print the pruned CSS output to console directly (for piping)
        (print-to-console! output-css))
      (process-exit!))))


;; -----------------------------------------------------------------------------
;; CLI Commands

;; What other commands here?
;; - prune    "Build a CSS file using only matching snowflake classes"
;; - eject    "Remove all snowflake hashes"
;; - init     "Create a snowflake-css.json config file"
;; - lonely   "Show a list of all lonely snowflake classes"
;; - report   "Print a report of how many classes are recognized in which files"
;; - scramble "Scramble snowflake hashes"

;; FIXME: snowflake.js banana should show "banana is an unrecognized command"

(def js-prune-command
  (js-obj
    "command" "prune"
    "describe" (str "Takes a CSS file as input and removes any ruleset that references "
                    "a snowflake class selector not found in template files")
    "handler" prune!))


(def js-report-command
  (js-obj
    "command" "report"
    "describe" "Prints a report of snowflake classes in your project."
    "handler" report!))

;; NOTE: candidate for deletion
; (def js-rename-command
;   (js-obj
;     "command" "rename <from> <to>"
;     "describe" "Rename a snowflake class"
;     "handler" (partial command-handler :rename)))
;
; ;; what other names for this? list, show, print
; (def js-list-command
;   (js-obj
;     "command" "list"
;     "describe" "List all known snowflake classes"
;     "handler" (partial command-handler :rename)))

;; -----------------------------------------------------------------------------
;; Main Entry Point

(def default-config-file "./snowflake-css.json")

(defn- main [& js-args]
  (timbre/merge-config! {:output-fn logging/format-log-msg})
  (doto yargs
    (.config)
    (.default "config" default-config-file)
    (.command js-prune-command)
    (.command js-report-command)
    ; (.command js-rename-command)
    ; (.command js-list-command)
    (.demandCommand) ;; show them --help if they do not pass in a command
    (.help)
    (.-argv)))
