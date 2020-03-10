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

(defn glob-sync [pattern]
  (js->clj (.sync glob pattern)))

;; -----------------------------------------------------------------------------
;; Main Entry Point

(defn- main [& js-args]
  (timbre/merge-config! {:output-fn format-log-msg})
  (let [args (js->clj (oget yargs "argv") :keywordize-keys true)

        css-search (:css args)
        css-is-file? (.isFileSync fs css-search)
        templates-path (:templates args)
        templates-path-is-file? (.isFileSync fs templates-path)

        ;; TODO: add an "overwrite-css-file" option that only works if they provide
        ;; a single CSS file as an option

        ;; FIXME: if they pass in --output-css-file and it is the same thing as
        ;; -css-input then throw a warning

        css-files (if css-is-file?
                    [css-search]
                    (glob-sync css-search))
        templates-files (if templates-path-is-file?
                          [templates-path]
                          (glob-sync templates-path))

        _ (timbre/info "Reading CSS and template files for snowflake classes …")
        css-classes (reduce
                      (fn [acc file]
                        (assoc acc file (get-snowflake-classes-from-file file)))
                      {}
                      css-files)
        template-classes (reduce
                           (fn [acc file]
                             (assoc acc file (get-snowflake-classes-from-file file)))
                           {}
                           templates-files)

        all-css-classes (apply set/union (vals css-classes))
        all-template-classes (apply set/union (vals template-classes))
        orphan-css-classes (set/difference all-css-classes all-template-classes)
        orphan-template-classes (set/difference all-template-classes all-css-classes)

        first-css-file-contents (.readFileSync fs (first css-files) "utf8")]

    (timbre/info "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    (timbre/info "Results:")
    (timbre/info "Found" (count all-css-classes) "snowflake classes in" (count css-classes) "CSS target files")
    (timbre/info "Found" (count all-template-classes) "snowflake classes in" (count template-classes) "template target files")
    (when-not (empty? orphan-template-classes)
      (timbre/warn (count orphan-template-classes) "template classes not found in CSS:" (vec orphan-template-classes)))
    (when-not (empty? orphan-css-classes)
      (timbre/warn (count orphan-css-classes) "classes not found in CSS:" (vec orphan-css-classes)))


    ;; TODO: log file size before / after here and the number of rules removed
    (when (:output-css-file args)
      (let [new-css-file (remove-classes-from-css first-css-file-contents orphan-css-classes)]
        (.writeFileSync fs (:output-css-file args) new-css-file "utf8")))))
