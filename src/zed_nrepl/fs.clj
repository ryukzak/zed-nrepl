(ns zed-nrepl.fs
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn extract-namespace-from-file [file-name]
  (when file-name
    (when (.exists (io/file file-name))
      (with-open [rdr (io/reader file-name)]
        (let [first-line (first (line-seq rdr))]
          (when (and first-line
                     (str/starts-with? first-line "(ns "))
            (-> first-line
                (str/replace #"^\(ns\s+" "")
                (str/replace #"\s.*$" ""))))))))

#_(extract-namespace-from-file "./src/zed_nrepl/core.clj")
