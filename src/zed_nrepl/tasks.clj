(ns zed-nrepl.tasks
  (:require
   [cheshire.core :as json]
   [cheshire.factory :as json.factory]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [zed-nrepl.tasks :as tasks]))

(defn parse-jsonc [content]
  (binding [json.factory/*json-factory* (json.factory/make-json-factory
                                         {:allow-comments true})]
    (json/parse-string content true)))

(defn read-tasks [filename]
  (when (.exists (io/file filename))
    (-> (slurp filename)
        (parse-jsonc))))

(defn curl-task [base-url task-name path params]
  {:label (str task-name " (zed-repl)")
   :command "curl",
   :args
   ["--request"
    "POST"
    (str base-url path)
    "--header"
    "'Content-Type: application/json'"
    "--data"
    "\"{"
    (->> params
         (map (fn [[k v]]
                (cond
                  (str/ends-with? (name k) "_base64")
                  (str "\\\"" (name k) "\\\": \\\"$( echo -n \"$" v "\" | base64 )\\\"")
                  :else
                  (str "\\\"" (name k) "\\\": \\\"$" v "\\\""))))
         (str/join ","))
    " }\""],
   :use_new_terminal false,
   :reveal_target "dock",
   :reveal "no_focus",
   :show_summary false,
   :hide "never",
   :shell "system",
   :allow_concurrent_runs false})

(defn zed-repl-tasks [base-url]
  [(curl-task base-url
              "Eval selected code" "/eval"
              {:file "ZED_FILE"
               :code_base64 "ZED_SELECTED_TEXT"})
   (curl-task base-url
              "Eval code at point" "/eval-at-point"
              {:file "ZED_FILE"
               :column "ZED_COLUMN"
               :row "ZED_ROW"})
   (curl-task base-url
              "Eval file" "/eval-file"
              {:file "ZED_FILE"})])

(defn add-zed-repl-tasks [tasks-file base-url timestamp]
  (let [tasks    (read-tasks tasks-file)
        task-set (->> (zed-repl-tasks base-url)
                      (map :label)
                      (into #{}))
        new-tasks (->> tasks
                       (remove (fn [t] (contains? task-set (:label t))))
                       (concat (zed-repl-tasks base-url)))
        new-json  (json/generate-string new-tasks {:pretty true})]
    (cond
      (= tasks new-tasks) :skip

      (some? tasks)
      (let [backup-file (str tasks-file "." timestamp ".bak")]
        (io/copy (io/file tasks-file) (io/file backup-file))
        (spit tasks-file new-json)
        :updated)

      :else
      (do (io/make-parents tasks-file)
          (spit tasks-file new-json)
          :created))))

(comment (add-zed-repl-tasks ".zed/tasks.json"
                             "http://localhost:3000"
                             (System/currentTimeMillis)))
