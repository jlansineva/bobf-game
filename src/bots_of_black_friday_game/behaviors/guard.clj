(ns bots-of-black-friday-game.behaviors.guard
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]))

(defn commit-attack
  [self {:keys [player]} state]
  (let [{:keys [affect]} player]
    (prn :attack!>)
    (affect state [:hurt 2])))

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
  [self {:keys [player]} state]
  (let [guard-position (get-in state [:entities :data self :position])
        targets [player]
        chase-target (some #(when (< (utils/get-distance guard-position (:position %)) 40) (:id %))
                           targets)]
    (assoc-in state
              [:entities :data self :chase-target]
              chase-target)))

(defn invincibility-frames-countdown
  [self _ state]
  (let [self-data (get-in state [:entities :data self])]
    (if (> (:invincibility self-data) 0)
      (update-in state [:entities :data self :invincibility] dec)
      state)))

(def guard-effects
  {::commit-attack commit-attack
   ::chase-player chase-player
   ::seek-target seek-target
   ::queue-for-removal queue-for-removal
   ::attack-cooling-down attack-cooling-down
   ::invincibility-frames-countdown invincibility-frames-countdown})

(defn dead?
  [{:keys [self]}]
  (<= (:health self) 0))

(defn player-too-far
  [{:keys [self required]}]
  (let [{:keys [player]} required
        target player]
    (if target
      (>
        (utils/get-distance (:position self)
                             (:position target))
        40)
      true)))

(defn player-at-chase-distance
  [{:keys [self required]}]
  (let [{:keys [player]} required
        target player]
    (if target
      (<= (utils/get-distance (:position self)
                              (:position target)) 40)
      false)))

(defn player-at-attack-distance
  [{:keys [self required]}]
  (let [{:keys [player]} required
        target player]
    (if target
      (<= (utils/get-distance (:position self)
                              (:position target)) 2)
      false)))

(defn attack-done
  [{:keys [self]}]
  (<= (get-in self [:cooldown :counter]) 0))

(defn chase-target
  [{:keys [self]}]
  (some? (:chase-target self)))

(def guard-evaluations
  {::attack-done attack-done
   ::dead dead?
   ::player-too-far player-too-far
   ::player-at-attack-distance player-at-attack-distance
   ::player-at-chase-distance player-at-chase-distance
   ::chase-target chase-target})

(defn hurt
  [state entity [damage]]
  (if (<= (:invincibility entity) 0)
    (-> state
        (update-in [:entities :data (:id entity) :health] - damage)
        (assoc-in [:entities :data (:id entity) :invincibility] 5))
    state))

(def guard-affections
  {:hurt hurt})

(def guard-fsm
  {:pre {:transitions [{:when [::dead]
                        :switch :dead}]}
   :always [::invincibility-frames-countdown] ;; TODO: Does nothing
   :current {:state :idle :effect ::seek-target}
   :require [:clock :player]
   :states {:chasing-player {:effect [::chase-player ::invincibility-frames-countdown]
                             :transitions [{:when [::player-too-far]
                                            :switch :idle}
                                           {:when [::player-at-attack-distance]
                                            :switch :attacking-player
                                            :post-effect ::commit-attack}]}
            :dead {:effect ::queue-for-removal
                   :transitions []}
            :idle {:effect [::seek-target ::invincibility-frames-countdown]
                   :transitions [{:when [::player-at-chase-distance ::chase-target]
                                  :switch :chasing-player}]}
            :attacking-player {:effect ::attack-cooling-down
                               :transitions [{:when [::player-at-chase-distance ::attack-done]
                                              :switch :chasing-player}]}}})

(def guard-entity
  {:id :generate/guard
   :type :guard
   :texture :enemy
   :speed 3
   :invincibility 0
   :chase-target nil
   :cooldown {:counter 1 :max 1}
   :position {:x 0 :y 0}
   :health 20})
