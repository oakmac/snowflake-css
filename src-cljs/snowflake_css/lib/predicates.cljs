(ns snowflake-css.lib.predicates
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as timbre]))

(defn has-a-digit? [s]
  (not= (.search s #"\d") -1))

(assert (has-a-digit? "5"))
(assert (has-a-digit? "a1"))
(assert (has-a-digit? "19"))
(assert (not (has-a-digit? "")))
(assert (not (has-a-digit? "aaa")))

(defn has-a-hex-char? [s]
  (not= (.search s #"[abcdef]") -1))

(assert (has-a-hex-char? "a"))
(assert (has-a-hex-char? "az"))
(assert (not (has-a-hex-char? "z")))

(def hash-length 5)

(defn valid-hash-part? [hash-str]
  (and (= hash-length (count hash-str))
       (has-a-digit? hash-str)
       (has-a-hex-char? hash-str)
       (= (.search hash-str #"[^abcdef0-9]") -1)))

(assert (valid-hash-part? "12aab"))
(assert (valid-hash-part? "ef552"))
(assert (not (valid-hash-part? "12aak")) "valid-hash: only hexadecimal chars")
(assert (not (valid-hash-part? "12aaB")) "valid-hash: no capital letters")
(assert (not (valid-hash-part? "bdaae")) "valid-hash: at least one number")
(assert (not (valid-hash-part? "14365")) "valid-hash: at least one letter")
(assert (not (valid-hash-part? "abcd343")) "valid-hash: too long")
(assert (not (valid-hash-part? "ab44")) "valid-hash: too short")

(defn- valid-snowflake-chars? [snowflake-class]
  (= (.search snowflake-class #"[^a-zA-Z0-9-]") -1))

(defn snowflake-class? [snowflake-class]
  (let [parts (str/split snowflake-class "-")
        hash-part (last parts)]
    (and
      (valid-snowflake-chars? snowflake-class)
      (valid-hash-part? hash-part))))

(assert (snowflake-class? "fizzle-44ebc"))
(assert (snowflake-class? "fizzle-wizzle-ae98f"))
(assert (not (snowflake-class? "fizzle-44ebca")))
(assert (not (snowflake-class? "fizzle-44373")))
(assert (not (snowflake-class? "fizzle-44y73")))
(assert (not (snowflake-class? "fizzle-aaabb")))
(assert (not (snowflake-class? "abc fizzle-44ebc")))
