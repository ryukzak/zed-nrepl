(ns zed-nrepl.core-test
  (:require [zed-nrepl.core :as core]
            [clojure.test :refer [deftest is testing]]))

(deftest extract-namespace-from-file-test
  (testing "Extracting namespace from file"
    (let [temp-file (java.io.File/createTempFile "test" ".clj")]
      (try
        (spit temp-file "(ns test.namespace.example\n  (:require [clojure.string :as str]))")
        (is (= "test.namespace.example" (core/extract-namespace-from-file (.getPath temp-file))))
        (finally
          (.delete temp-file))))

    (testing "Returns nil for non-existent file"
      (is (nil? (core/extract-namespace-from-file "non-existent-file.clj"))))

    (testing "Returns nil for file without namespace"
      (let [temp-file (java.io.File/createTempFile "test" ".clj")]
        (try
          (spit temp-file "(defn example [] :example)")
          (is (nil? (core/extract-namespace-from-file (.getPath temp-file))))
          (finally
            (.delete temp-file)))))))

(deftest decode-test
  (testing "Base64 decoding"
    (is (= "Hello, World!" (core/decode "SGVsbG8sIFdvcmxkIQ==")))
    (is (= "" (core/decode "")))))
