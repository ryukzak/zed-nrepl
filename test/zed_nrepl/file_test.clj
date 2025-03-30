(ns zed-nrepl.file-test
  (:require [clojure.test :refer [deftest is testing]]
            [zed-nrepl.file :as file]))

(deftest at-point-test
  (is (= nil (file/at-point "./test/zed_nrepl/file_test.clj" 0 0)))
  (is (= '(ns zed-nrepl.file-test
            (:require [clojure.test :refer [deftest is testing]] [zed-nrepl.file :as file]))
         (file/at-point "./test/zed_nrepl/file_test.clj" 1 1)))

  (testing "ns"
    (is (= 'ns
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 2)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 3)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 4))))

  (testing "zed-nrepl.file-test"
    (is (= 'zed-nrepl.file-test
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 5)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 6)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 24)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 24)
           (file/at-point "./test/zed_nrepl/file_test.clj" 1 50)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 1)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 2))))

  (is (= '(:require [clojure.test :refer [deftest is testing]] [zed-nrepl.file :as file])
         (file/at-point "./test/zed_nrepl/file_test.clj" 2 3)))

  (testing ":refer [deftest is testing]"
    (is (= '[deftest is testing]
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 34)))

    (is (= 'deftest
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 40)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 41)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 42)))
    (is (= 'is
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 43)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 45)))
    (is (= 'testing
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 46)
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 53)))

    (is (= '[deftest is testing]
           (file/at-point "./test/zed_nrepl/file_test.clj" 2 54))))

  (is (= '[clojure.test :refer [deftest is testing]]
         (file/at-point "./test/zed_nrepl/file_test.clj" 2 55))))

(deftest test-get-ns
  (testing "Extracting namespace from Clojure file content"
    (is (= "my.namespace" (file/get-ns-from-content "(ns my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content "(ns   my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content "(ns \n my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content ";; some comment\n(ns my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content "  ;; some code\n(ns my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content "(def x 1)\n(ns my.namespace)")))
    (is (= "my.namespace" (file/get-ns-from-content ";; (ns other.namespace)\n(ns my.namespace)")))
    (is (nil? (file/get-ns-from-content "no namespace here")))))

(deftest test-extract-namespace-from-file
  (testing "Extracting namespace from existing Clojure file"
    (is (string? (file/get-ns "./src/zed_nrepl/core.clj")))
    (is (nil? (file/get-ns "non-existent-file.clj")))))
