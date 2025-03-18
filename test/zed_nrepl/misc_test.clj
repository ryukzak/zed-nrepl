(ns zed-nrepl.misc-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [zed-nrepl.misc :as misc]))

(deftest decode-test
  (testing "Base64 decoding"
    (is (= "Hello, World!" (misc/decode64 "SGVsbG8sIFdvcmxkIQ==")))
    (is (= "" (misc/decode64 "")))))
