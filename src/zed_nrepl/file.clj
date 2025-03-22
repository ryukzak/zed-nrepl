(ns zed-nrepl.file
  (:require
   [clojure.java.io :as io]
   [rewrite-clj.zip :as z]))

(defn get-ns-from-content [content]
  (let [zloc (z/of-string content)]
    (some-> (z/find-value zloc z/next 'ns)
            (z/next)
            (z/sexpr)
            (str))))

(defn get-ns [file-name]
  (when (.exists (io/file file-name))
    (let [content (slurp file-name)]
      (get-ns-from-content content))))

#_(extract-namespace-from-file "./src/zed_nrepl/core.clj")

(defn at-point [file-name col pos]
  (let [content (slurp file-name)
        zloc (z/of-string content
                          {:track-position? true})]
    (z/sexpr
     (loop [zloc zloc]
       (let [[[c1 p1] [c2 p2]] (z/position-span zloc)
             zleft             (z/left zloc)
             zdown             (z/down zloc)
             zright            (z/right zloc)]
         (cond
           ;; inside
           (or (< c1 col c2)
               (cond
                 (= c1 col c2) (<= p1 pos p2)
                 (= c1 col)    (<= p1 pos)
                 (= col c2)    (<= pos p2)))
           (if (or (nil? zdown) ;; not sexp or bracket
                   (and (= c1 col) (= p1 pos))
                   (and (= c2 col) (= p2 pos)))
             zloc
             (recur zdown))

           ;; between sexps or at the last
           (or (and (= col c2) (< pos p2))
               (< col c2))
           zleft

           :else (recur zright)))))))

#_(at-point "./src/zed_nrepl/core.clj" 111 36)
