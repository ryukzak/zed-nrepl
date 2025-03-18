(ns zed-nrepl.fs
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

;; FIXME: rewrite-clj?

(defn get-ns [text]
  (some-> (re-find (re-matcher #"\([\s\n]*ns[\s\n]+[^\s\n();]+" text))
          (str/replace #"\(ns[\s\n]+" "")))

(defn extract-namespace-from-file [file-name]
  (when (and file-name (.exists (io/file file-name)))
    (-> (slurp file-name)
        (get-ns))))

#_(extract-namespace-from-file "./src/zed_nrepl/core.clj")
