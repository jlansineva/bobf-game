(ns bots-of-black-friday-game.behaviors.shopper
  (:require [clojure-bot.log :as log]
            [clojure-bot.map :as maps]
            [pelinrakentaja-engine.dev.tila :as tila]))

(def move-matrix {[0 1] "DOWN"
                  [0 -1] "UP"
                  [1 0] "RIGHT"
                  [-1 0] "LEFT"})

(defn get-move-based-on-route
  [route position]
  (let [{px :x py :y} position
        {rx :x ry :y} (first route)]
    (get move-matrix [(- rx px) (- ry py)])))

(defn get-next-move
  [{:keys [player level]
    :as game-state} target]
  (let [position-local (get-in player [:local :position])
        position-server (get-in player [:server :position])
        route-to-target (maps/find-route
                          (get-in level [(:y position-local) (:x position-local)])
                          (:position target)
                          level)
        next-move (get-move-based-on-route route-to-target position-local)
        ;; if position at server is different from local, we have moved and can send a next move
        queue? true #_(or
                 (not= (:x position-local) (:x position-server))
                 (not= (:y position-local) (:y position-server)))]
    (cond-> game-state
      true (assoc-in [:player :local :current-target] target)
      queue? (assoc-in [:player :local :position] position-server)
      queue? (assoc-in [:action-queue] next-move))))

(defn move-to-exit
  [{:keys [level] :as game-state}]
  (let [best-target (get-in level [:exit])]
    (get-next-move game-state {:position best-target})))

(defn discounted-price
  [price discount-percent]
  (- price (* price (/ discount-percent 100))))

(defn affordable-items
  [items money]
  (remove #(let [{:keys [price discountPercent]} %]
             (> (discounted-price price discountPercent)
               money))
    items))

(defn suitable-target
  [items {:keys [money current-item] :as _player}]
  (let [affordable-items (affordable-items items money)]
    (first
      (sort-by :discountPercent > (keep identity (conj affordable-items current-item))))))

(defn move-to-closest-potion
  [{:keys [player]
    :as game-state}]
  (let [{:keys [potions]} (maps/process-targets (get-in game-state [:items]))
        position-local (get-in player [:local :position])
        potions-by-distance (map #(assoc % :estimated-distance (maps/heuristic position-local (:position %))) potions)
        best-target (first (sort-by :estimated-distance potions-by-distance))]
    (get-next-move game-state best-target)))

(defn move-to-closest-affordable-item
  [game-state]
  (let [player (get-in game-state [:player :local])
        {:keys [purchase-candidates]} (maps/process-targets (get-in game-state [:items]))
        best-target (suitable-target purchase-candidates player)]
    (get-next-move game-state best-target)))

(defn pick-item
  [game-state]
  (assoc-in game-state [:action-queue] "PICK"))

(def effects {::no-op identity
              ::move-to-closest-affordable-item move-to-closest-affordable-item
              ::move-to-closest-potion move-to-closest-potion
              ::move-to-exit move-to-exit
              ::pick-item pick-item
              ::initialize (fn [game-state]
                             (assoc-in game-state [:player :local :initialized?] true))
              ::dead (fn [game-state]
                       (assoc-in game-state [:player :local :alive?] false))})

(def evaluations {::low-health (fn [game-state]
                                 (let [health (get-in game-state [:player :server :health])]
                                   (when health
                                     (< health 70))))
                  ::no-potions (fn [game-state]
                                 (let [{:keys [potions]} (maps/process-targets
                                                           (get-in game-state [:items]))]
                                   (empty? potions)))
                  ::on-item (fn [game-state]
                              (let [{target-position :position} (get-in game-state [:player :local :current-target])
                                    server-position (get-in game-state [:player :server :position])]
                                (maps/same-node? target-position server-position)))
                  ::enough-money (fn [game-state]
                                   (let [{:keys [price discountPercent]} (get-in game-state [:player :local :current-target])
                                         money (get-in game-state [:player :server :money])]
                                     (when (and price discountPercent)
                                       (> money (discounted-price price discountPercent)))))
                  ::affordable-items (fn [game-state]
                                       (let [{:keys [purchase-candidates]} (maps/process-targets
                                                                             (get-in game-state [:items]))]
                                         (and
                                           (seq purchase-candidates)
                                           (seq (affordable-items purchase-candidates (get-in game-state [:player :server :money]))))))
                  ::no-affordable-items (fn [game-state]
                                          (let [{:keys [purchase-candidates]} (maps/process-targets
                                                                                (get-in game-state [:items]))]
                                            (and
                                              (seq purchase-candidates)
                                              (nil? (seq (affordable-items purchase-candidates (get-in game-state [:player :server :money])))))))
                  ::on-health (fn [game-state]
                                (let [current-target (get-in game-state [:player :local :current-target :position])
                                      server-position (get-in game-state [:player :server :position])]
                                  (maps/same-node? current-target server-position)))
                  ::item-picked (fn [game-state]
                                  (let [items (get-in game-state [:items])
                                        current-target (get-in game-state [:player :local :current-target :position])]
                                    (nil? (some #(when (maps/same-node? current-target (:position %)) %) items))))
                  ::dead (fn [game-state]
                           (let [server-instance (get-in game-state [:player :server])
                                 alive? (get-in game-state [:player :local :alive?])]
                             (and
                               alive?
                               (nil? server-instance))))
                  ::initialized (fn [game-state]
                                  (true? (get-in game-state [:player :local :initialized?])))
                  ::instance-found (fn [game-state]
                                     (some? (get-in game-state [:player :server])))
                  ::potions-available (fn [game-state]
                                        (let [{:keys [potions]} (maps/process-targets
                                                                  (get-in game-state [:items]))]
                                          (seq potions)))})

(def bot-logic-state
  {:pre {:transitions [{:when [::dead ::initialized]
                        :switch :dead}]}
   :current {:state :waiting-for-init
             :effect ::no-op}
   :last {:state nil}
   :states {:dead {:effect ::dead
                   :transitions []}
            :waiting-for-init {:effect ::no-op
                               :transitions [{:when [::instance-found]
                                              :switch :idle
                                              :post-effect ::initialize}]}
            :idle {:effect ::no-op
                   :transitions [{:when [::low-health ::no-potions]
                                  :switch :move-to-exit}
                                 {:when [::low-health ::potions-available]
                                  :switch :move-to-health}
                                 {:when [::affordable-items]
                                  :switch :move-to-item}]}
            :move-to-item {:effect ::move-to-closest-affordable-item
                           :transitions [{:when [::low-health ::potions-available]
                                          :switch :move-to-health}
                                         {:when [::low-health ::no-potions]
                                          :switch :move-to-exit}
                                         {:when [::on-item ::enough-money]
                                          :switch :pick-item}]}
            :move-to-exit {:effect ::move-to-exit
                           :transitions [{:when [::affordable-items]
                                          :switch :move-to-item}]}
            :move-to-health {:effect ::move-to-closest-potion
                             :transitions [{:when [::on-health]
                                            :switch :pick-item}]}
            :pick-item {:effect ::pick-item
                        :transitions [{:when [::item-picked ::no-affordable-items]
                                       :switch :move-to-exit}
                                      {:when [::item-picked ::affordable-items]
                                       :switch :idle}
                                      {:when [::item-picked ::potions-available]
                                       :switch :idle}]}}})
