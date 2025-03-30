(ns zed-nrepl.tasks-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [zed-nrepl.tasks :as tasks]))

;; Test constants

(def test-dir (str (System/getProperty "java.io.tmpdir") "/zed-nrepl-test"))
(def test-tasks-file (str test-dir "/tasks.json"))
(def test-timestamp 1000000000000)

;; Helper functions for tests

(defn setup-test-dir []
  (let [dir (io/file test-dir)]
    (.mkdirs dir)
    dir))

(defn cleanup-test-dir []
  (let [dir (io/file test-dir)]
    (when (.exists dir)
      (doseq [f (file-seq dir)]
        (when (and (.isFile f) (.exists f))
          (.delete f)))
      (.delete dir))))

(defn write-test-tasks [content]
  (spit test-tasks-file content))

(defn has-zed-repl-task? [tasks]
  (some #(= (:label %) "Eval selected code (zed-repl)") tasks))

;; Tests

(deftest parse-jsonc-test
  (testing "Parsing JSON with comments"
    (let [jsonc "{
      // This is a comment
      \"key\": \"value\",
      /* Multi-line
         comment */
      \"nested\": {
        \"array\": [1, 2, 3] // inline comment
      }
    }"
          result (tasks/parse-jsonc jsonc)]
      (is (= result {:key "value", :nested {:array [1 2 3]}}))
      (is (= (get-in result [:nested :array]) [1 2 3])))))

(deftest read-tasks-test
  (testing "Reading non-existent file returns nil"
    (is (nil? (tasks/read-tasks (str test-dir "/nonexistent.json")))))

  (testing "Reading valid tasks file"
    (setup-test-dir)
    (try
      (write-test-tasks "[{\"label\": \"Test Task\", \"command\": \"echo\"}]")
      (is (= (tasks/read-tasks test-tasks-file)
             [{:label "Test Task", :command "echo"}]))
      (finally (cleanup-test-dir)))))

(deftest add-zed-repl-tasks-test
  (testing "Creating new tasks file when it doesn't exist"
    (setup-test-dir)
    (try
      (let [result (tasks/add-zed-repl-tasks test-tasks-file test-timestamp)]
        (is (= result :created))
        (is (.exists (io/file test-tasks-file)))
        (let [tasks (tasks/read-tasks test-tasks-file)]
          (is (has-zed-repl-task? tasks) "Tasks should include zed-repl task")
          (is (= 2 (count tasks)))))
      (finally (cleanup-test-dir))))

  (testing "Updating existing tasks file without zed-repl task"
    (setup-test-dir)
    (try
      (write-test-tasks "[{\"label\": \"Existing Task\", \"command\": \"echo\"}]")
      (let [result (tasks/add-zed-repl-tasks test-tasks-file test-timestamp)]
        (is (= result :updated))
        (is (.exists (io/file (str test-tasks-file "." test-timestamp ".bak"))))
        (let [tasks (tasks/read-tasks test-tasks-file)]
          (is (has-zed-repl-task? tasks) "Tasks should include zed-repl task")
          (is (= 3 (count tasks)))
          (is (some #(= (:label %) "Existing Task") tasks))))
      (finally (cleanup-test-dir))))

  (testing "Skipping update when zed-repl task already exists with same config"
    (setup-test-dir)
    (try
      ;; First add the task
      (tasks/add-zed-repl-tasks test-tasks-file test-timestamp)
      ;; Then attempt to add it again
      (let [result (tasks/add-zed-repl-tasks test-tasks-file test-timestamp)]
        (is (= result :skip)))
      (finally (cleanup-test-dir)))))

(deftest zed-repl-tasks-structure-test
  (testing "Zed REPL tasks have required fields"
    (let [task (get tasks/zed-repl-tasks "Eval selected code (zed-repl)")]
      (is (string? (:label task)))
      (is (string? (:command task)))
      (is (vector? (:args task)))
      (is (contains? task :reveal_target))
      (is (contains? task :reveal))
      (is (contains? task :use_new_terminal)))))
