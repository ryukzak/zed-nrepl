(ns zed-nrepl.repl
  (:require
   [clojure.core.async :refer [chan  <!! >! go close!]]
   [clojure.edn :as edn]
   [nrepl.core :as nrepl]
   [zed-nrepl.file :as file]))

(defn spit-port [port]
  (spit ".nrepl-port" (str port)))

(defn slurp-port []
  (try
    (Integer/parseInt (slurp ".nrepl-port"))
    (catch Exception _ nil)))

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
  (let [responses (nrepl/message client
                                 {:op "eval"
                                  :code (str '(->> (all-ns)
                                                   (map ns-name)
                                                   (map name)))})]
    (->> responses
         (keep :value)
         first
         (edn/read-string)
         (into #{}))))

(defn eval-by-nrepl [{host :host port :port timeout :timeout}
                     {file :file code :code}]
  (let [timeout (or timeout 1000)
        file-ns (file/get-ns file)]
    (with-open [conn (nrepl/connect :host host :port port)]
      (let [client     (nrepl/client conn timeout)
            namespaces (all-repl-namespaces client)
            resp (->> (nrepl/message client (cond-> {:op "eval"
                                                     :code code}
                                              (contains? namespaces file-ns)
                                              (assoc :ns file-ns)))
                      (combine-responses)
                      doall)]
        resp))))

(defn eval-by-nrepl-chan [{host :host port :port timeout :timeout}
                          {file :file code :code}]
  (let [timeout (or timeout 1000)
        file-ns (file/get-ns file)
        c       (chan 40)]
    (go
      (with-open [conn (nrepl/connect :host host :port port)]
        (let [client     (nrepl/client conn timeout)
              namespaces (all-repl-namespaces client)
              ns-loaded? (contains? namespaces file-ns)
              ns         (if ns-loaded? file-ns "user")

              responses
              (nrepl/message client {:op   "eval"
                                     :code (or code
                                               (slurp file))
                                     :ns   ns})]
          (>! c ns)
          (doseq [response responses]
            (>! c response))
          (close! c))))
    {:ns   (<!! c)
     :chan c}))

(comment
  (def c (eval-by-nrepl-chan {:host "localhost" :port (slurp-port) :timeout 1000}
                             {:code "(+ 40 2)"}))
  (<!! c)
  (println (seq
            (eval-by-nrepl {:host "localhost" :port (slurp-port) :timeout 1000} {:code "(+ 40 2)"}))))
