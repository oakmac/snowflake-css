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
  (= (.search snowflake-class #"[^a-z0-9-]") -1))

(defn snowflake-class? [possible-snowflake-class]
  (let [parts (str/split possible-snowflake-class "-")
        hash-part (last parts)]
    (and
      (>= (count parts) 2)
      (valid-hash-part? hash-part)
      (valid-snowflake-chars? possible-snowflake-class))))

(assert (snowflake-class? "fizzle-44ebc"))
(assert (snowflake-class? "fizzle-wizzle-ae98f"))
(assert (not (snowflake-class? " fizzle-44ebc")))
(assert (not (snowflake-class? "fizzle-44ebc ")))
(assert (not (snowflake-class? "fizzle-44ebc{")))
(assert (not (snowflake-class? ".fizzle-44ebc")))
(assert (not (snowflake-class? "fizzle-44ebca")))
(assert (not (snowflake-class? "fizzle-44373")))
(assert (not (snowflake-class? "fizzle-44y73")))
(assert (not (snowflake-class? "fizzle-aaabb")))
(assert (not (snowflake-class? "abc fizzle-44ebc")))

;; examples from the README
(assert (snowflake-class? "primary-btn-d4b50"))
(assert (snowflake-class? "header-411db"))
(assert (snowflake-class? "login-btn-9c2da"))
(assert (snowflake-class? "cancel-6b36a"))
(assert (snowflake-class? "jumbo-image-4b455"))
(assert (not (snowflake-class? "LoginBtn-783af")))
(assert (not (snowflake-class? "logo-22536")))
(assert (not (snowflake-class? "cancel-button-bceff")))
(assert (not (snowflake-class? "nav-link-e72c")))

(defn valid-cli-arg?
  "Is x a possible CLI argument?"
  [x]
  (and (string? x)
       (not (str/blank? x))))

(assert (valid-cli-arg? "foo"))
(assert (valid-cli-arg? "foo/bar"))
(assert (not (valid-cli-arg? "")))
(assert (not (valid-cli-arg? nil)))
