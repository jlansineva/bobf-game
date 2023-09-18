(ns bots-of-black-friday-game.behaviors.fireball
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]))

(defn countdown-zero
  [{:keys [self]}]
  (<= (:life self) 0))

(defn hit-entity
  [{:keys [self required]}]
  (let [{:keys [player guard mammon]} required]
    (case (:layer self)
      :enemy-projectile
      (< (utils/get-distance (:position self) (:position player)) 2)

      :player-projectile
      (some (fn [e]
              (when (< (utils/get-distance (:position self) (:position e)) 2) true))
            (into [] (keep identity (conj (vals guard) mammon))))
      false)))

(def fireball-evaluations
  {::countdown-zero countdown-zero
   ::hit-entity hit-entity})

(defn move-fireball
  [self {:keys [clock]} state]
  (let [movement (get-in state [:entities :data self :movement])]
    (-> state
        (update-in [:entities :data self :life] - (:delta-time clock))
        (update-in [:entities :data self :position :x] + (* (:x movement) (:delta-time clock)))
        (update-in [:entities :data self :position :y] + (* (:y movement) (:delta-time clock))))))

(defn set-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn hurt-entity
  [self {:keys [player mammon guard]} state]
  (let [self-data (get-in state [:entities :data self])
        state (case (:layer self-data)
                :enemy-projectile (let [{:keys [affect]} player]
                                    (affect state [:hurt (:damage self-data)]))
                :player-projectile (let [hit-entities
                                         (filter
                                           (fn [e]
                                            (< (utils/get-distance (:position self-data) (:position e)) 2))
                                           (into [] (keep identity (conj (vals guard) mammon))))]
                                     (reduce (fn [state entity]
                                               (let [{:keys [affect]} entity]
                                                 (affect state [:hurt (:damage self-data)])))
                                             state
                                             hit-entities))
                state)]
    state))

(def fireball-effects
  {::move-fireball move-fireball
   ::set-for-removal set-for-removal
   ::hurt-entity hurt-entity})

(def fireball-fsm
  {:require [:clock :player :mammon [:type :guard]]
   :current {:state :moving :effect ::move-fireball}
   :last {:state nil}
   :states {:moving {:effect ::move-fireball
                     :transitions [{:when [::countdown-zero]
                                    :switch :extinguished}
                                   {:when [::hit-entity]
                                    :switch :extinguished
                                    :post-effect ::hurt-entity}]}
            :extinguished {:effect ::set-for-removal
                           :transitions []}}})

(def fireball-entity
  {:id :generate/fireball
   :speed 8
   :damage 10
   :life 5
   :type :projectile
   :layer :none
   :texture :weapon
   :position {:x 0 :y 0}
   :movement {:x 0 :y 0}})

(defn with-pos-speed-layer
  [start target layer]
  (-> fireball-entity
      (assoc :position start
             :layer layer
             :movement (utils/get-movement-vector start target (:speed fireball-entity)))))
