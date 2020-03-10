(ns snowflake-css.cli.core
  (:require
    ["fs-plus" :as fs]
    ["glob" :as glob]
    ["postcss" :as postcss]
    ["yargs" :as yargs]
    [clojure.string :as str]
    [clojure.set :as set]
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







(def possibles-regex #"([a-z0-9]+-){1,}([abcdef0-9]){5}")

(defn- get-hashes-from-file [file]
  ; (timbre/info "Reading" file "…")
  (let [file-contents (.readFileSync fs file "utf8")
        hashes (->> (re-seq possibles-regex file-contents)
                    (map first)
                    (filter snowflake-class?)
                    set)]
    (timbre/info "Found" (count hashes) "snowflake classes in" file)
    hashes))




(defn print-node [x]
  (js/console.log (oget x "selector"))
  (js/console.log "~~~~~~~~~~~~~~~~~~~~"))



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

        css-files (if css-is-file?
                    [css-search]
                    (glob-sync css-search))
        templates-files (if templates-path-is-file?
                          [templates-path]
                          (glob-sync templates-path))

        _ (timbre/info "Reading CSS and template files for snowflake classes …")
        css-classes (reduce
                      (fn [acc file]
                        (assoc acc file (get-hashes-from-file file)))
                      {}
                      css-files)
        template-classes (reduce
                           (fn [acc file]
                             (assoc acc file (get-hashes-from-file file)))
                           {}
                           templates-files)

        all-css-classes (apply set/union (vals css-classes))
        all-template-classes (apply set/union (vals template-classes))
        orphan-template-classes (vec (set/difference all-template-classes all-css-classes))]


    (timbre/info "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    (timbre/info "Results:")
    (timbre/info "Found" (count all-css-classes) "snowflake classes in" (count css-classes) "CSS target files")
    (timbre/info "Found" (count all-template-classes) "snowflake classes in" (count template-classes) "template target files")
    (when-not (empty? orphan-template-classes)
      (timbre/warn (count orphan-template-classes) "template classes not found in CSS:" orphan-template-classes))))

    ; (timbre/info "zzzzzzzzzzzzzzzzzzzzzzzzzzzzz")))

   ; (timbre/info css-classes)
   ; (timbre/info "444444444444444444444444444444444444444444444444444")
   ; (timbre/info template-classes)))


   ; (assert css-is-file? "FIXME: allow glob patterns for CSS files eventually")
   ; (assert search-path-is-file? "FIXME: allow glob patterns for search path eventually")))

   ; (let [css-hashes (get-hashes-from-file css-search)
   ;       search-hashes (get-hashes-from-file search-path)
   ;       in-css-not-in-search (set/difference css-hashes search-hashes)
   ;       in-search-not-in-css (set/difference search-hashes css-hashes)]


         ; file-contents (.readFileSync fs css-search "utf8")
         ; js-root (.parse postcss file-contents)]

     ; (timbre/info (count css-hashes))
     ; (timbre/info (count search-hashes))
     ; (timbre/info in-css-not-in-search)
     ; (timbre/info "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
     ; (timbre/info in-search-not-in-css))))



     ; (.walkRules js-root print-node))))

   ; (timbre/info "I will search:" css-search)
   ; (timbre/info "I will search:" search-path)
   ; (timbre/info css-is-file?)
   ; (timbre/info "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")))
