(ns bots-of-black-friday-game.behaviors.flame
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]))

(defn life-less-than-zero
  [{:keys [self]}]
  (<= (:life self) 0))

(def flame-evaluations
  {::life-less-than-zero life-less-than-zero})

(defn set-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn hurt-player-when-close
  [state self player]
  (let [{:keys [affect]} player]
    (cond-> state
      (< (utils/get-distance (:position self) (:position player)) 2)
      (affect [:hurt (:damage self)]))))

(defn count-life-down
  [self {:keys [clock player]} state]
  (let [self-data (get-in state [:entities :data self])
        state (case (:layer self-data)
                :enemy-projectile (hurt-player-when-close state self-data player)
                state)]
    (update-in state [:entities :data self :life] - (:delta-time clock))))

(def flame-effects
  {::set-for-removal set-for-removal
   ::count-life-down count-life-down})

(def flame-fsm
  {:require [:clock :player]
   :current {:state :life-countdown :effect ::count-life-down}
   :last {:state nil}
   :states {:life-countdown {:effect ::count-life-down
                             :transitions [{:when [::life-less-than-zero]
                                            :switch :extinguished}]}
            :extinguished {:effect ::set-for-removal
                           :transitions []}}})

(def flame-entity
  {:id :generate/flame
   :type :projectile
   :life 10
   :damage 2
   :layer :none
   :texture :enemy
   :position {:x 0 :y 0}})

(defn with-pos-layer
  [position layer]
  (assoc flame-entity
         :position position
         :layer layer))
