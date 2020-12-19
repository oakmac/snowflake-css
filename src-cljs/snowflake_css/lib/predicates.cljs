(ns snowflake-css.lib.predicates
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as timbre]))

(defn- has-a-digit? [s]
  (not= (.search s #"\d") -1))

(defn- has-a-hex-char? [s]
  (not= (.search s #"[abcdef]") -1))

(def hextail-length 5)

(defn- valid-hextail? [hextail]
  (and (= hextail-length (count hextail))
       (has-a-digit? hextail)
       (has-a-hex-char? hextail)
       (= (.search hextail #"[^abcdef0-9]") -1)))

(defn- valid-snowflake-chars? [snowflake-class]
  (= (.search snowflake-class #"[^a-z0-9-]") -1))

;; -----------------------------------------------------------------------------
;; Public API

(defn snowflake-class? [possible-snowflake-class]
  (let [parts (str/split possible-snowflake-class "-")
        hextail (last parts)]
    (and
      (>= (count parts) 2)
      (valid-hextail? hextail)
      (valid-snowflake-chars? possible-snowflake-class))))

(defn valid-cli-arg?
  "Is x a possible CLI argument?"
  [x]
  (and (string? x)
       (not (str/blank? x))))

;; -----------------------------------------------------------------------------
;; Testing

(assert (has-a-digit? "5"))
(assert (has-a-digit? "a1"))
(assert (has-a-digit? "19"))
(assert (not (has-a-digit? "")))
(assert (not (has-a-digit? "aaa")))

(assert (has-a-hex-char? "a"))
(assert (has-a-hex-char? "az"))
(assert (not (has-a-hex-char? "z")))

(assert (valid-hextail? "12aab"))
(assert (valid-hextail? "ef552"))
(assert (not (valid-hextail? "12aak")) "valid-hextail: only hexadecimal chars")
(assert (not (valid-hextail? "12aaB")) "valid-hextail: no capital letters")
(assert (not (valid-hextail? "bdaae")) "valid-hextail: at least one number")
(assert (not (valid-hextail? "14365")) "valid-hextail: at least one letter")
(assert (not (valid-hextail? "abcd343")) "valid-hextail: too long")
(assert (not (valid-hextail? "ab44")) "valid-hextail: too short")

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

(assert (valid-cli-arg? "foo"))
(assert (valid-cli-arg? "foo/bar"))
(assert (not (valid-cli-arg? "")))
(assert (not (valid-cli-arg? nil)))
