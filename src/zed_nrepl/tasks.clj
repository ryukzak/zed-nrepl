(ns zed-nrepl.tasks
  (:require
   [cheshire.core :as json]
   [cheshire.factory :as json.factory]
   [clojure.java.io :as io]
   [zed-nrepl.tasks :as tasks]))

(defn parse-jsonc [content]
  (binding [json.factory/*json-factory* (json.factory/make-json-factory
                                         {:allow-comments true})]
    (json/parse-string content true)))

(defn read-tasks [filename]
  (when (.exists (io/file filename))
    (-> (slurp filename)
        (parse-jsonc))))

(def zed-repl-tasks
  {"Eval selected code (zed-repl)"
   {:label "Eval selected code (zed-repl)",
    :command "curl",
    :args
    ["--request"
     "POST"
     "http://localhost:3000/eval"
     "--header"
     "'Content-Type: application/json'"
     "--data"
     "\"{"
     "\\\"file\\\": \\\"$ZED_FILE\\\","
     "\\\"code_base64\\\": "
     "\\\"$( echo -n \"$ZED_SELECTED_TEXT\" | base64 )\\\""
     " }\""],
    :use_new_terminal false,
    :reveal_target "dock",
    :reveal "always",
    :show_summary false,

    :hide "never",

    :shell "system",
    :allow_concurrent_runs false}})

(defn add-zed-repl-tasks [tasks-file timestamp]
  (let [tasks     (read-tasks tasks-file)
        new-tasks (->> tasks
                       (remove (fn [t] (contains? zed-repl-tasks (:label t))))
                       (concat (vals zed-repl-tasks)))
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

(comment (add-zed-repl-tasks ".zed/tasks.json" (System/currentTimeMillis)))
