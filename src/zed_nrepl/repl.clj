(ns zed-nrepl.repl
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [nrepl.core :as nrepl]
   [zed-nrepl.fs :as fs]))

(defn collect-response-value [{:keys [value] :as msg}]
  (cond
    (and (string? value)
         (str/starts-with? value "#"))
    msg

    (string? value)
    (try
      (assoc msg :value (edn/read-string value))
      (catch Exception _ msg))

    :else
    msg))

(defn combine-responses [responses]
  (reduce
   (fn [m [k v]]
     (case k
       (:id :ns) (assoc m k v)
       :value (update-in m [k] (fnil conj []) v)
       :status (update-in m [k] (fnil into #{}) v)
       :session (update-in m [k] (fnil conj #{}) v)
       (if (string? v)
         (update-in m [k] #(str % v))
         (assoc m k v))))
   {} (apply concat responses)))

(defn all-repl-namespaces [client]
  (->> (nrepl/message client
                      {:op "eval"
                       :code (str '(->> (all-ns)
                                        (map ns-name)
                                        (map name)))})
       (map collect-response-value)
       combine-responses
       :value
       (apply concat)
       (into #{})))

(defn eval-by-nrepl [{host :host port :port}
                     {file :file code :code}]
  (let [file-ns (fs/extract-namespace-from-file file)]
    (with-open [conn (nrepl/connect :host host :port port)]
      (let [client     (nrepl/client conn 1000)
            namespaces (all-repl-namespaces client)
            resp (->> (-> client
                          (nrepl/message (cond-> {:op "eval"
                                                  :code code}
                                           (contains? namespaces file-ns)
                                           (assoc :ns file-ns))))
                      (map collect-response-value)
                      combine-responses
                      doall)]
        resp))))
