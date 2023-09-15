(ns bots-of-black-friday-game.behaviors.fireball
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]))

(defn countdown-zero
  [{:keys [self]}]
  (<= (:life self) 0))

(defn hit-player
  [state]
  false)

(def fireball-evaluations
  {::countdown-zero countdown-zero
   ::hit-player hit-player})

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

(def fireball-effects
  {::move-fireball move-fireball
   ::set-for-removal set-for-removal})

(def fireball-fsm
  {:require [:clock :player]
   :current {:state :moving :effect ::move-fireball}
   :last {:state nil}
   :states {:moving {:effect ::move-fireball
                     :transitions [{:when [::countdown-zero]
                                    :switch :extinguished}
                                   {:when [::hit-player]
                                    :switch :extinguished}]}
            :extinguished {:effect ::set-for-removal
                           :transitions []}}})

(def fireball-entity
  {:id :generate/fireball
   :speed 8
   :damage 10
   :life 5
   :type :fireball
   :texture :weapon
   :position {:x 0 :y 0}
   :movement {:x 0 :y 0}})

(defn fireball-with-pos-and-speed
  [start target]
  (-> fireball-entity
      (assoc :position start
             :movement (utils/get-movement-vector start target (:speed fireball-entity)))))
