(ns bots-of-black-friday-game.behaviors.firewall
  (:require [bots-of-black-friday-game.behaviors.utils :as utils]
            [bots-of-black-friday-game.behavior :as behavior]
            [bots-of-black-friday-game.behaviors.flame :as flame]))

(defn countdown-zero
  [{:keys [self]}]
  (<= (:life self) 0))

(defn hit-player
  [state]
  false)

(defn spawn-flame-cooldown-done
  [{:keys [self]}]
  (<= (:flame-cooldown self) 0))

(def firewall-evaluations
  {::countdown-zero countdown-zero
   ::hit-player hit-player
   ::spawn-flame-cooldown-done spawn-flame-cooldown-done})

(defn move-firewall
  [self {:keys [clock]} state]
  (let [movement (get-in state [:entities :data self :movement])]
    (-> state
        (update-in [:entities :data self :flame-cooldown] dec)
        (update-in [:entities :data self :life] - (:delta-time clock))
        (update-in [:entities :data self :position :x] + (* (:x movement) (:delta-time clock)))
        (update-in [:entities :data self :position :y] + (* (:y movement) (:delta-time clock))))))

(defn set-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn spawn-flame
  [self required state]
  (let [self-data (get-in state [:entities :data self])]
    (-> state
        (assoc-in [:entities :data self :flame-cooldown] (get-in state [:entities :data self :max]))
        (behavior/add-behavioral-entity (assoc flame/flame-entity :position (:position self-data))
                                        flame/flame-fsm
                                        flame/flame-effects
                                        flame/flame-evaluations))))

(def firewall-effects
  {::move-firewall move-firewall
   ::set-for-removal set-for-removal
   ::spawn-flame spawn-flame})

(def firewall-fsm
  {:require [:clock :player]
   :current {:state :moving :effect ::move-firewall}
   :last {:state nil}
   :states {:moving {:effect ::move-firewall
                     :transitions [{:when [::spawn-flame-cooldown-done]
                                    :switch :spawn-flame}
                                   {:when [::countdown-zero]
                                            :switch :extinguished}
                                           {:when [::hit-player]
                                            :switch :extinguished}]}
            :spawn-flame {:effect ::spawn-flame
                          :transitions [{:when [:true]
                                         :switch :moving}]}
            :extinguished {:effect ::set-for-removal
                           :transitions []}}})

(def firewall-entity
  {:id :generate/firewall
   :speed 6
   :damage 10
   :life 5
   :type :firewall
   :flame-cooldown 10 :max 10
   :texture :potion
   :position {:x 0 :y 0}
   :movement {:x 0 :y 0}})

(defn firewall-with-pos-and-speed
  [start target]
  (-> firewall-entity
      (assoc :position start
             :movement (utils/get-movement-vector start target (:speed firewall-entity)))))
