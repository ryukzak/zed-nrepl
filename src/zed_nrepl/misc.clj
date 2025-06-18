(ns zed-nrepl.misc
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [zed-nrepl.misc :as misc])
  (:import
   java.util.Base64))

(defn decode64 [to-decode]
  (String. (.decode (Base64/getDecoder)
                    to-decode)))

(defn encode64 [to-encode]
  (.encodeToString (Base64/getEncoder)
                   (.getBytes to-encode "UTF-8")))

(defn pp [m]
  (with-out-str (pprint/pprint m)))

(def return-symbol "⏎")

(defn return-suffix [s]
  (str (str/trimr s)
       return-symbol
       "\n"))

(defn pp-return [m]
  (return-suffix
   (binding [pprint/*print-right-margin* 140]
     (with-out-str (pprint/pprint m)))))
