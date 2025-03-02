(ns zed-nrepl.core
  (:require
   [bidi.ring :refer [make-handler]]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [nrepl.core :as nrepl]
   [nrepl.server :as nrepl.server]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.util.response :refer [response]]
   [zed-nrepl.tasks :as tasks])
  (:import
   java.util.Base64))

(declare nrepl-server)

(defn decode [to-decode]
  (String. (.decode (Base64/getDecoder)
                    to-decode)))

(defn pp [m]
  (with-out-str (pprint/pprint m)))

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

#_(extract-namespace-from-file "/Users/ryukzak/src/zed-nrepl/src/zed_nrepl/core.clj")

(defn collect-response-value [{:keys [value] :as msg}]
  (cond
    (and (string? value)
         (str/starts-with? value "#"))
    msg

    (string? value)
    (try
      (prn value)
      (assoc msg :value (read-string value))
      (catch Exception e
        (throw (IllegalStateException. (str "Could not read response value: " value) e))))

    :else
    msg))

(defn eval-by-nrepl [{file :file
                      code :code}]
  (let [nrepl-port (:port @nrepl-server)
        cur-ns (extract-namespace-from-file file)]

    (with-open [conn (nrepl/connect :host "localhost" :port nrepl-port)]
      (->> (-> (nrepl/client conn 1000)
               (nrepl/message {:op "eval"
                               :code code
                               :ns cur-ns}))
           (map collect-response-value)
           nrepl/combine-responses))))

(comment
  (eval-by-nrepl {:code "(+ 1 2)"})
  (eval-by-nrepl {:code "(def x 12)"})
  (eval-by-nrepl {:code "x"})
  (eval-by-nrepl {:code "*ns*"})
  (eval-by-nrepl {:code "*ns* (in-ns 'core) request0"}))

(defn eval-handler [request]
  (let [code (or (some-> request :body :code-base64 decode)
                 (-> request :body :code))
        file (some-> request :body :file)
        result (eval-by-nrepl {:file file
                               :code code})

        promt (str/trim (str (:ns result) "=> " code))]

    (->> [promt
          (->> (:value result)
               (map pp)
               (str/join "\n"))
          (pp (dissoc result :id :session :value))]
         (str/join "\n")
         response)))

(def routes
  ["/" {"eval" {:post #'eval-handler}}])

(def app
  (-> (make-handler routes)
      (wrap-json-body {:key-fn #(-> (str/replace % "_" "-")
                                    keyword)})
      wrap-json-response))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce nrepl-server (atom nil))

(defn stop-nrepl-server []
  (when @nrepl-server
    (nrepl.server/stop-server @nrepl-server)
    (reset! nrepl-server nil)))

(defn start-nrepl-server []
  (stop-nrepl-server)
  (reset! nrepl-server
          (let [server (nrepl.server/start-server)]
            (spit ".nrepl-port" (:port server))
            (println "nREPL server started on port" (:port server))
            server)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce bridge-server (atom nil))

(defn stop-bridge-server []
  (when @bridge-server
    (.stop @bridge-server)
    (reset! bridge-server nil)))

(defn start-bridge-server []
  (stop-bridge-server)
  (reset! bridge-server (run-jetty app {:port 3000 :join? false})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main []
  (println "Add zed-repl to .zed/tasks.json: "
           (tasks/add-zed-repl-tasks ".zed/tasks.json" (System/currentTimeMillis)))

  (println "Start nREPL server")
  (start-nrepl-server)

  (println "Start http -- nREPL bridge server")
  (start-bridge-server))

(comment
  (start-bridge-server)
  (-main))
