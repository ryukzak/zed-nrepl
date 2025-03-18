(ns zed-nrepl.fs-test
  (:require [clojure.test :refer [deftest is testing]]
            [zed-nrepl.fs :as fs]))

(deftest test-get-ns
  (testing "Extracting namespace from Clojure file content"
    (is (= "my.namespace" (fs/get-ns "(ns my.namespace)")))
    (is (= "my.namespace" (fs/get-ns "(ns   my.namespace)")))
    (is (= "my.namespace" (fs/get-ns "(ns \n my.namespace)")))
    (is (= "my.namespace" (fs/get-ns ";; some comment\n(ns my.namespace)")))
    (is (= "my.namespace" (fs/get-ns "  ;; some code\n(ns my.namespace)")))
    (is (= "my.namespace" (fs/get-ns "(def x 1)\n(ns my.namespace)")))
    #_(is (= "my.namespace" (fs/get-ns ";; (ns other.namespace)\n(ns my.namespace)")))
    (is (nil? (fs/get-ns "no namespace here")))))

(deftest test-extract-namespace-from-file
  (testing "Extracting namespace from existing Clojure file"
    (is (string? (fs/extract-namespace-from-file "./src/zed_nrepl/core.clj")))
    (is (nil? (fs/extract-namespace-from-file "non-existent-file.clj")))))
