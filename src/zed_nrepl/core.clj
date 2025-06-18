(ns zed-nrepl.core
  (:require
   [bidi.ring :refer [make-handler]]
   [clojure.core.async :refer [<!!]]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.term.colors :as colors]
   [nrepl.server :as nrepl.server]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.util.io]
   [ring.util.response :refer [response]]
   [zed-nrepl.file :as file]
   [zed-nrepl.misc :as misc]
   [zed-nrepl.repl :as repl]
   [zed-nrepl.tasks :as tasks]))

(declare nrepl-server)

(defn promt [ns code]
  (let [max-code-len 160
        code (str/trim code)
        code (if (< (count code) max-code-len)
               (str code)
               (str (subs code 0 (- max-code-len 10))
                    "\n... "
                    (subs code (- (count code) 5))))]
    (str ns (colors/bold (colors/green "=> ")) code "\n")))

(defn repl-out [msg]
  (cond
    (:out msg) (:out msg)

    (and (string? (:value msg))
         (str/starts-with? (:value msg) "#"))
    (let [edn (str/replace-first (:value msg) #"^[^ ]+ " "")]
      (try
        (misc/pp-return (edn/read-string edn))
        (catch Exception _ (misc/return-suffix (:value msg)))))

    (contains? msg :value)
    (colors/bold (misc/pp-return (edn/read-string (:value msg))))

    (:err msg) (str "Error:\n"
                    (->> (str/split (:err msg) #"\n")
                         (map #(str "    " %))
                         (str/join "\n"))
                    "\n")
    (:ex msg) (str "Exception:\n"
                   "    " (:ex msg) "\n"
                   "Root exception:\n"
                   "    " (:root-ex msg) "\n")

    (= (:status msg) ["done"]) nil

    :else (str msg "\n")))

(defn- eval-response [promt ch]
  (response (ring.util.io/piped-input-stream
             (fn [ostream]
               (let [w (io/make-writer ostream {})]
                 (.write w promt)
                 (loop [msg (<!! ch)]
                   (when msg
                     (when-let [out (repl-out msg)]
                       (.write w out)
                       (.flush w))
                     (recur (<!! ch)))))))))

(defn eval-handler [request]
  (let [code (or (some-> request :body :code-base64 misc/decode64)
                 (-> request :body :code))
        file (some-> request :body :file)
        {ns :ns
         ch :chan} (repl/eval-by-nrepl-chan
                    {:host "localhost" :port (:port @nrepl-server)}
                    {:file file :code code})]

    (eval-response (promt ns (or code
                                 (str "file:" file)))
                   ch)))

(defn eval-at-point-handler [request]
  (let [file (some-> request :body :file)
        row (some-> request :body :row Integer/parseInt)
        column (some-> request :body :column Integer/parseInt)
        code (with-out-str (pr (file/at-point file row column)))
        {ns :ns
         ch :chan} (repl/eval-by-nrepl-chan
                    {:host "localhost" :port (:port @nrepl-server)}
                    {:file file :code code})]
    (eval-response (promt ns code) ch)))

(defn eval-file-handler [request]
  (let [file (some-> request :body :file)
        {ns :ns
         ch :chan} (repl/eval-by-nrepl-chan
                    {:host "localhost" :port (:port @nrepl-server)}
                    {:file file :code (slurp file)})]
    (eval-response (promt ns (format "(eval (slurp %s))" file)) ch)))

(def routes
  ["/" {"eval" {[:action] {:post #'eval-handler}
                :post #'eval-handler}
        "eval-at-point" {:post #'eval-at-point-handler}
        "eval-file" {:post #'eval-file-handler}}])

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
            (repl/spit-port (:port server))
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
  (reset! bridge-server (run-jetty app {:port 3000
                                        :join? false
                                        :output-buffer-size 1})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main []
  (println "Add zed-repl to .zed/tasks.json: "
           (tasks/add-zed-repl-tasks ".zed/tasks.json"
                                     "http://localhost:3000"
                                     (System/currentTimeMillis)))

  (println "Start nREPL server")
  (start-nrepl-server)

  (println "Start http -- nREPL bridge server")
  (start-bridge-server))

(comment
  (-main))
