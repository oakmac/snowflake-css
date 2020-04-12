(ns snowflake-css.lib.flake-parsing
  (:require
    ["postcss" :as postcss]
    [clojure.set :as set]
    [clojure.string :as str]
    [oops.core :refer [oget]]
    [snowflake-css.lib.filesystem-helpers :refer [file-exists? read-file-sync!]]
    [snowflake-css.lib.predicates :refer [snowflake-class?]]
    [taoensso.timbre :as timbre]))

(def non-flake-char-regex #"[^a-z0-9-]")

(defn string->flakes
  "returns a set of snowflake classes from a string"
  [a-string]
  (as-> a-string $
    (str/split $ non-flake-char-regex)
    (filter snowflake-class? $)
    (set $)))

(assert (= (string->flakes "fizzle-44ebc") #{"fizzle-44ebc"}))
(assert (= (string->flakes "fizzle-44ebc{") #{"fizzle-44ebc"}))
(assert (= (string->flakes "fizzle-44ebc.") #{"fizzle-44ebc"}))
(assert (= (string->flakes "}fizzle-44ebc") #{"fizzle-44ebc"}))
(assert (= (string->flakes ".fizzle-44ebc a") #{"fizzle-44ebc"}))
(assert (= (string->flakes ".fizzle-44ebc a, .foo-56bec") #{"fizzle-44ebc" "foo-56bec"}))
(assert (= (string->flakes "fizzle-44ebc foo-56bec") #{"fizzle-44ebc" "foo-56bec"}))
(assert (= (string->flakes "<div class=\"fizzle-44ebc\">") #{"fizzle-44ebc"}))
(assert (= (string->flakes "<div class=fizzle-44ebc>") #{"fizzle-44ebc"}))
(assert (= (string->flakes "<div class=\"fizzle-44ebc foo-56bec\">") #{"fizzle-44ebc" "foo-56bec"}))
(assert (= (string->flakes "e4d2ad4f-f4f3-420b-ba95-d2b869fc9a6d") #{}))
(assert (= (string->flakes "      'e4d2ad4f-f4f3-420b-ba95-d2b869fc9a6d'") #{}))

(defn file->flakes
  "Reads a file and returns a set of the snowflake classes found in it"
  ([file]
   (file->flakes file false))
  ([file log?]
   (if-not (file-exists? file)
     (do
       (timbre/warn "Cannot read file:" file)
       #{})
     (let [file-contents (read-file-sync! file)
           flakes (string->flakes file-contents)]
       (when log?
         (timbre/info "Found" (count flakes) "flakes in" file))
       flakes))))

(defn files->flakes-map
  "returns a map of filename --> snowflake classes"
  ([files]
   (files->flakes-map files false))
  ([files log?]
   (reduce
     (fn [acc file]
       (assoc acc file (file->flakes file log?)))
     {}
     files)))

;; TODO: need to throw an error here if css-content cannot be parsed
(defn remove-flakes-from-css-string
  "Parses a string of CSS using postcss and removes any rulesets that
   1) contain a snowflake class and 2) not in flakes-to-keep-around
   ignores rulesets with selectors that contain no snowflake classes"
  [css-content flakes-to-keep-around]
  (let [root-node (.parse postcss css-content)]
    (.walkRules root-node
      (fn [js-node]
        (let [selector (oget js-node "selector")
              flakes-in-selector (string->flakes selector)]
          ;; 1) only look at rulesets that contain snowflake classes
          ;; 2) remove any ruleset that does not exist in flakes-to-keep-around
          (when (and (not (empty? flakes-in-selector))
                     (empty? (set/intersection flakes-in-selector flakes-to-keep-around)))
            (.remove js-node)))))
    (oget (.toResult root-node) "css")))
