(ns zed-nrepl.core
  (:require
   [bidi.ring :refer [make-handler]]
   [clojure.string :as str]
   [nrepl.server :as nrepl.server]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
   [ring.util.response :refer [response]]
   [zed-nrepl.misc :as misc]
   [zed-nrepl.repl :as repl]
   [zed-nrepl.tasks :as tasks]))

(declare nrepl-server)

(defn eval-handler [request]
  (let [code (or (some-> request :body :code-base64 misc/decode64)
                 (-> request :body :code))
        file (some-> request :body :file)
        result (repl/eval-by-nrepl
                {:host "localhost" :port (:port @nrepl-server)}
                {:file file :code code})

        promt (str/trim (str (:ns result) "=> " code))]

    (->> [promt
          (->> (:value result)
               (map misc/pp)
               (str/join "\n"))
          (misc/pp (dissoc result :id :session :value))]
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
