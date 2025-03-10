(ns zed-nrepl.misc
  (:require
   [clojure.pprint :as pprint]
   [zed-nrepl.misc :as misc])
  (:import
   java.util.Base64))

(defn decode64 [to-decode]
  (String. (.decode (Base64/getDecoder)
                    to-decode)))

(defn pp [m]
  (with-out-str (pprint/pprint m)))
