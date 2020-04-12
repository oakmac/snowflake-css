(ns snowflake-css.lib.filesystem-helpers
  (:require
    ["fs-plus" :as fs]
    ["glob" :as glob]
    [clojure.set :as set]
    [taoensso.timbre :as timbre]))

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

(defn glob-pattern->files
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
