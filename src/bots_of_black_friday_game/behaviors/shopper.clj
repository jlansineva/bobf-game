(ns bots-of-black-friday-game.behaviors.shopper
  (:require [bots-of-black-friday-game.map :as maps]
            [bots-of-black-friday-game.behavior :as behavior]
            [clojure.math :as math]
            [clojure.pprint :as pp]))

(defn get-move-based-on-route
  [route position]
  (let [{px :x py :y} position
        {rx :x ry :y} (first route)]
    {:dx (* (- rx (math/round px)) 5)
     :dy (* (- ry (math/round py)) 5)}))

(defn get-next-move
  [self {:keys [map-data]
         :as game-state} target]
  (let [position (:position self)
        target-position (:position target)
        route-to-target (maps/find-route
                         (get-in map-data [:map-data (math/round (:y position)) (math/round (:x position))])
                         target-position
                         (:map-data map-data))
        next-move (get-move-based-on-route route-to-target position)
        ;; if position at server is different from local, we have moved and can send a next move
        ]
    next-move))

(defn move-to-exit
  [self {:keys [clock]} {:keys [map-data] :as state}]
  (let [self (get-in state [:entities :data self])
        exit-target (get-in map-data [:exit])
        next-move (get-next-move self state {:position exit-target})]
    (-> state
        (assoc-in [:entities :data (:id self) :current-target] {:position exit-target})
        (update-in [:entities :data (:id self) :position :x] + (* (:dx next-move) (:delta-time clock)))
        (update-in [:entities :data (:id self) :position :y] + (* (:dy next-move) (:delta-time clock))))))

(defn suitable-target
  [items]
  (nth items (rand-int (count items))))

(defn move-to-an-item
  [self {:keys [item clock]} state]
  #_(prn :move-to-closest> self item)
  (let [{:keys [current-target] :as self} (get-in state [:entities :data self])
        items (into [] (vals item))
        best-target (if (nil? (:id current-target))
                      (suitable-target items)
                      current-target)
        next-move (get-next-move self state best-target)]
    (-> state
        (assoc-in [:entities :data (:id self) :current-target] best-target)
        (update-in [:entities :data (:id self) :position :x] + (* (:dx next-move) (:delta-time clock)))
        (update-in [:entities :data (:id self) :position :y] + (* (:dy next-move) (:delta-time clock))))))

(defn pick-item
  [self required state]
  (let [{:keys [current-target]} (get-in state [:entities :data self])]
    (-> state
        (update-in [:entities :data self :items] conj current-target)
        (assoc-in [:entities :data self :current-target] {})
        (update-in [:entities :removal-queue] (comp vec conj) current-target))))

(defn pay-items-and-exit
  [self required state]
  (let [self (get-in state [:entities :data self])
        total-price (reduce (fn [total {:keys [price]}]
                              (+ total price)) 0 (:items self))]
    (prn :piae> (:items self))
    (prn :piae> total-price)
    (update-in state [:entities :data :level :money-collected] + total-price)))

(defn dead
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn clean-current-target
  [self required state]
  (assoc-in state [:entities :data self :current-target] {}))

(def shopper-effects
  {::move-to-an-item move-to-an-item
   ::move-to-exit move-to-exit
   ::pick-item pick-item
   ::pay-items-and-exit pay-items-and-exit
   ::dead dead
   ::clean-current-target clean-current-target})

(defn on-item
  [{:keys [self required] :as game-state}]
  (let [target-position (:current-target self)
        entity-position (:position self)]
    (maps/same-node? (:position target-position) entity-position)))

(defn not-enough-items
  [{:keys [self required] :as gs}]
  (let [{:keys [item]} required]
    (and
     (seq (keys item))
     (<= (count (:items self)) 3))))

(defn enough-items
  [{:keys [self required]}]
  (let [{:keys [item]} required]
    (and
     (seq (keys item))
     (> (count (:items self)) 3))))

(defn item-picked
  [{:keys [self]}]
  (let [{:keys [current-target]} self]
    (empty? (keys current-target))))

(defn dead?
  [{:keys [self]}]
  (let [{:keys [alive?]} self]
    (not alive?)))

(defn at-exit
  [{:keys [self]}]
  (let [{:keys [current-target]} self]
    (maps/same-node? (:position current-target) (:position self))))

(defn item-gone [{:keys [self required]}]
  (let [{:keys [current-target]} self
        {:keys [item]} required]
    (nil? (some #(when (= (:id current-target) (:id %)) %) (vals item)))))

(def shopper-evaluations
  {::on-item on-item
   ::not-enough-items not-enough-items
   ::enough-items enough-items
   ::item-picked item-picked
   ::dead dead?
   ::at-exit at-exit
   ::item-gone item-gone})

(def shopper-fsm
  {:pre {:transitions [{:when [::dead]
                        :switch :dead}]}
   :require [[:type :item] :clock]
   :current {:state :idle
             :effect :no-op}
   :last {:state nil}
   :states {:dead {:effect ::dead
                   :transitions []}
            :idle {:effect :no-op
                   :transitions [{:when [::enough-items]
                                  :switch :move-to-exit}
                                 {:when [::not-enough-items]
                                  :switch :move-to-item}]}
            :move-to-item {:effect ::move-to-an-item
                           :transitions [{:when [::item-gone]
                                          :switch :switch-target}
                                         {:when [::on-item]
                                          :switch :pick-item}]}
            :move-to-exit {:effect ::move-to-exit
                           :transitions [{:when [::at-exit]
                                          :switch :pay-items-and-exit}]}
            :switch-target {:effect ::clean-current-target
                            :transitions [{:when [:true]
                                           :switch :idle}]}
            :pay-items-and-exit {:effect ::pay-items-and-exit
                                 :transitions [{:when [:true]
                                                :switch :dead}]}
            :pick-item {:effect ::pick-item
                        :transitions [{:when [::item-picked ::enough-items]
                                       :switch :move-to-exit}
                                      {:when [::item-picked]
                                       :switch :idle}]}}})

(def shopper-entity
  {:current-target {:x 0 :y 0 :id nil}
   :position {:x 15 :y 15}
   :id :generate/shopper
   :texture :enemy
   :type :shopper
   :items []
   :alive? true})
