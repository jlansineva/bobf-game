(ns bots-of-black-friday-game.behaviors.guard
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]))

(defn commit-attack
  [self required state]
  (prn :attack!>)
  state)

(defn chase-player
  [self {:keys [clock]} state]
  (let [target (get-in state [:entities :data self :chase-target])
        self-data (get-in state [:entities :data self])
        target-position (get-in state [:entities :data target :position])
        movement (utils/get-movement-vector (:position self-data) target-position (:speed self-data))]
    (-> state
        (update-in [:entities :data self :position :x] + (* (:x movement) (:delta-time clock)))
        (update-in [:entities :data self :position :y] + (* (:y movement) (:delta-time clock))))))

(defn queue-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn attack-cooling-down
  [self {:keys [clock]} state]
  (update-in state [:entities :data self :cooldown :counter] - (:delta-time clock)))

(defn seek-target
  [self {:keys [testing]} state]
  (let [guard-position (get-in state [:entities :data self :position])
        targets (vals testing)]
    (assoc-in state [:entities :data self :chase-target]
              (some #(when (< (utils/get-distance guard-position (:position %)) 40) (:id %))
                    targets))))

(def guard-effects
  {::commit-attack commit-attack
   ::chase-player chase-player
   ::seek-target seek-target
   ::queue-for-removal queue-for-removal
   ::attack-cooling-down attack-cooling-down})

(defn dead?
  [{:keys [self]}]
  (<= (:health self) 0))

(defn player-too-far
  [{:keys [self required]}]
  (let [target (get-in required [:testing (:chase-target self)])]
    (if target
      (>
        (utils/get-distance (:position self)
                             (:position target))
        40)
      true)))

(defn player-at-chase-distance
  [{:keys [self required]}]
  (let [target (get-in required [:testing (:chase-target self)])]
    (if target
      (<= (utils/get-distance (:position self)
                              (:position target)) 40)
      false)))

(defn player-at-attack-distance
  [{:keys [self required]}]
  (let [target (get-in required [:testing (:chase-target self)])]
    (if target
      (<= (utils/get-distance (:position self)
                              (:position target)) 2)
      false)))

(defn attack-done
  [{:keys [self]}]
  (<= (get-in self [:cooldown :counter]) 0))

(def guard-evaluations
  {::attack-done attack-done
   ::dead dead?
   ::player-too-far player-too-far
   ::player-at-attack-distance player-at-attack-distance
   ::player-at-chase-distance player-at-chase-distance})

(def guard-fsm
  {:pre {:transitions [{:when [::dead]
                        :switch :dead}]}
   :current {:state :idle :effect ::seek-target}
   :require [:clock :player [:type :testing]]
   :states {:chasing-player {:effect ::chase-player
                             :transitions [{:when [::player-too-far]
                                            :switch :idle}
                                           {:when [::player-at-attack-distance]
                                            :switch :attacking-player
                                            :post-effect ::commit-attack}]}
            :dead {:effect ::queue-for-removal
                   :transitions []}
            :idle {:effect ::seek-target
                   :transitions [{:when [::player-at-chase-distance]
                                  :switch :chasing-player}]}
            :attacking-player {:effect ::attack-cooling-down
                               :transitions [{:when [::player-at-chase-distance ::attack-done]
                                              :switch :chasing-player}]}}})

(def guard-entity
  {:id :generate/guard
   :type :guard
   :texture :enemy
   :speed 3
   :chase-target nil
   :cooldown {:counter 1 :max 1}
   :position {:x 0 :y 0}
   :health 20})
