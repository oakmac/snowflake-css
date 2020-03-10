(ns snowflake-css.lib.predicates
  (:require
    [clojure.string :as str]
    [oops.core :refer [oget]]
    [taoensso.timbre :as timbre]))

(defn has-a-digit? [s]
  (not= (.search s #"\d") -1))

(defn has-a-hex-char? [s]
  (not= (.search s #"[abcdef]") -1))

(assert (has-a-digit? "5"))
(assert (has-a-digit? "a1"))
(assert (has-a-digit? "19"))
(assert (not (has-a-digit? "")))
(assert (not (has-a-digit? "aaa")))

(assert (has-a-hex-char? "a"))
(assert (has-a-hex-char? "az"))
(assert (not (has-a-hex-char? "z")))

(def hash-length 5)

(defn valid-hash-chars? [hash-str]
  (= (.search hash-str #"[^abcdef0-9]") -1))

(assert (valid-hash-chars? "aaaaaaaaaaa"))
(assert (valid-hash-chars? "abcd343"))
(assert (not (valid-hash-chars? "abcd343z")))
(assert (not (valid-hash-chars? "Ab44")))

;; TODO:
;; - should we check anything in the first part of the hash?
(defn snowflake-class? [txt]
  (let [dash (subs txt (- (count txt) (inc hash-length)) (- (count txt) hash-length))
        hash-part (subs txt (- (count txt) hash-length))]
    (boolean
      (and
        (= dash "-")
        (has-a-digit? hash-part)
        (has-a-hex-char? hash-part)
        (valid-hash-chars? hash-part)))))

(assert (snowflake-class? "fizzle-44ebc"))
(assert (snowflake-class? "fizzle-wizzle-ae98f"))
(assert (not (snowflake-class? "fizzle-44ebca")))
(assert (not (snowflake-class? "fizzle-44373")))
(assert (not (snowflake-class? "fizzle-44y73")))
(assert (not (snowflake-class? "fizzle-aaabb")))
