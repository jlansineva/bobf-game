(ns bots-of-black-friday-game.behaviors.flame)

(defn life-less-than-zero
  [{:keys [self]}]
  (<= (:life self) 0))

(def flame-evaluations
  {::life-less-than-zero life-less-than-zero})

(defn set-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn count-life-down
  [self {:keys [clock]} state]
  (update-in state [:entities :data self :life] - (:delta-time clock)))

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
   :type :flame
   :life 10
   :texture :enemy
   :position {:x 0 :y 0}})
