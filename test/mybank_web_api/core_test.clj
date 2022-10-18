(ns mybank-web-api.core-test
  (:require [clojure.test :refer :all]
            [mybank-web-api.core :refer :all]))

(deftest is-a-valid-number?-test
  (testing "Checking if there is any content at all"
    (are [x y] (= x y)
               false (is-a-valid-number? "")
               false (is-a-valid-number? " ")))
  (testing "Checking if the content is a valid number"
    (are [x y] (= x y)
               false  (is-a-valid-number? "120a")
               true   (is-a-valid-number? "120")
               true   (is-a-valid-number? "120.0"))))

(deftest saldo-suficiente?-test
  (let [conta {:saldo 100}]
    (testing "Check success"
      (are [x y] (= x (saldo-suficiente? conta y))
                  true 1
                  true 99
                  true 100)
    (testing "Check failure"
      (are [x] (= false (saldo-suficiente? conta x))
                 100.01
                 100.1
                 110)))))

(deftest positive?-test
  (testing "Checking if the number is greater than zero"
    (are [x y] (= x y)
               true   (positive? "0.1")
               false  (positive? "0.0")
               false  (positive?"-0.1"))))

(deftest sanitizer-test
  (testing "Checking if the return is a BigDecimal"
    (is (= java.math.BigDecimal (type (sanitizer "120.0")))))
  (testing "Checking if the return is the same value and a BigDecimal also"
    (is (= 120M (sanitizer "120.0")))))

(comment
  (run-tests)
  )