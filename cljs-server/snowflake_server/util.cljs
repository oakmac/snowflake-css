(ns snowflake-server.util
  (:require
    [clojure.set :refer [union]]
    [clojure.string :refer [split split-lines]]))

(def fs (js/require "fs-plus"))

;;------------------------------------------------------------------------------
;; Predicates
;;------------------------------------------------------------------------------

(def snowflake-regex #"([a-z0-9]+-){1,}([abcdef0-9]){5}")

(defn snowflake-class?
  "Is s a snowflake class?"
  [s]
  (let [the-hash (last (split s "-"))]
    (and (string? s)
         (.test snowflake-regex s) ;; must match this regex
         (.test #"\d" s)           ;; must have some numbers
         (.test #"[a-z]" s)        ;; must have some alpha characters
         (.test #"\d" the-hash)    ;; hash must contain at least one number
         (.test #"[a-z]" the-hash)))) ;; has must contain at least one alpha character

;;------------------------------------------------------------------------------
;; File Helpers
;;------------------------------------------------------------------------------

;; TODO: wrap this in a try/catch
(defn jsonfile->clj [f]
  (-> (.readFileSync fs f "utf8")
      js/JSON.parse
      (js->clj :keywordize-keys true)))

(defn read-flakes-from-file
  "Returns a set of flake maps from a file."
  [file]
  (let [file-contents (.readFileSync fs file "utf8")
        lines (split-lines file-contents)
        flakes (atom #{})]
    (dorun
      (map-indexed
        (fn [idx line]
          (let [new-flakes (re-seq snowflake-regex line)
                new-flakes (map first new-flakes)
                new-flakes (filter snowflake-class? new-flakes)]
            (doseq [f new-flakes]
              (swap! flakes conj {:flake f
                                  :file file
                                  :line-no (inc idx)}))))
        lines))
    (deref flakes)))

(defn read-flakes-from-files
  [files]
  (reduce
    (fn [flakes file]
      (union flakes (read-flakes-from-file file)))
    #{}
    files))
