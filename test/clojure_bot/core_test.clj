(ns clojure-bot.core-test
  (:require [clojure.test :refer :all]
            [clojure-bot.core :as core]))

(deftest generate-game-state-test
  (testing "Generation of game state"))

(deftest test-process-map
  (testing "Map processing"
    (let [tiles ["___x"
                 "__xx"
                 "____"
                 "_xx_"]]
      (is (= {0 {0 {:x 0, :y 0, :weight 1}
                  1 {:x 1, :y 0, :weight 1}
                  2 {:x 2, :y 0, :weight 1}
                  3 {:x 3, :y 0, :weight 999}}
              1 {0 {:x 0, :y 1, :weight 1}
                  1 {:x 1, :y 1, :weight 1}
                  2 {:x 2, :y 1, :weight 999}
                  3 {:x 3, :y 1, :weight 999}}
              2 {0 {:x 0, :y 2, :weight 1}
                  1 {:x 1, :y 2, :weight 1}
                  2 {:x 2, :y 2, :weight 1}
                  3 {:x 3, :y 2, :weight 1}}
              3 {0 {:x 0, :y 3, :weight 1}
                  1 {:x 1, :y 3, :weight 999}
                  2 {:x 2, :y 3, :weight 999}
                  3 {:x 3, :y 3, :weight 1}}}
            (core/process-map tiles))))))

(deftest test-heuristic
  (testing "Heuristic function"
    (let [start {:x 1 :y 1}
          end {:x 3 :y 3}]
      (is (= 4 (core/heuristic start end))))))
