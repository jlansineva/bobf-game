(ns bots-of-black-friday-game.behaviors.item-spawner
  (:require [bots-of-black-friday-game.behaviors.item :as item]
            [bots-of-black-friday-game.behavior :as behavior]))

(defn countdown-for-spawn
  [self required state]
  (let [{:keys [clock]} required
        countdown-decrease (* (:delta-time clock) 1.0)]
    (update-in state
               [:entities :data self :countdown]
               - countdown-decrease)))

(defn spawn-random-item
  [self {:keys [level] :as required} state]
  (prn :> :spawning-a-random-item required)
  (let [countdown-reset (assoc-in state [:entities :data self :countdown] 5)]
    (behavior/add-behavioral-entity
     countdown-reset
     (-> item/item-entity
         (update :position
                 assoc
                 :x (+ 2 (rand-int (- (:width level) 4)))
                 :y (+ 2 (rand-int  (- (:height level) 4))))
         (assoc :price (+ 1000 (* (rand-int 25) 100))))
     item/item-fsm
     item/item-effects
     item/item-evaluations
     item/item-affections)))

(def item-spawner-effects {::countdown-for-spawn countdown-for-spawn
                           ::spawn-random-item spawn-random-item})

(defn clock-paused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:paused? clock)))

(defn spawn-countdown-zero
  [{:keys [self required]}]
  (<= (:countdown self) 0))

(defn under-spawned-item-limit
  [{:keys [self required]}]
  (let [{:keys [item level]} required]
    (< (count item) 10 #_(:item-limit level))))

(defn clock-unpaused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:unpaused? clock)))

(defn boss-stage
  [{:keys [self required]}]
  (let [{:keys [level]} required]
                                                ;; TODO: once we figure out how levels work
    ))

(def item-spawner-evaluations {::clock-paused clock-paused
                               ::spawn-countdown-zero spawn-countdown-zero
                               ::under-spawned-item-limit under-spawned-item-limit
                               ::clock-unpaused clock-unpaused
                               ::boss-stage boss-stage})

(def item-spawner-fsm {:require [:clock :level [:type :item]]
                       :current {:state :idle
                                 :effect :no-op}
                       :last {:state nil}
                       :states {:idle {:effect :no-op
                                       :transitions [{:when [:true]
                                                      :switch :spawner-running}]}
                                :spawner-running {:effect ::countdown-for-spawn
                                                  :transitions [{:when [::clock-paused]
                                                                 :switch :paused}
                                                                {:when [::boss-stage]
                                                                 :switch :stopped}
                                                                {:when [::spawn-countdown-zero ::under-spawned-item-limit]
                                                                 :switch :spawning}]}
                                :spawning {:effect ::spawn-random-item
                                           :transitions [{:when [:true]
                                                          :switch :spawner-running}]}
                                :paused {:effect :no-op
                                         :transitions [{:when [::clock-unpaused]
                                                        :switch :spawner-running}]}
                                :stopped {:effect :no-op
                                          :transitions []}}})

(def item-spawner-entity {:position {:x 40 :y 15}
                          :id :item-spawner
                          :type :item-spawner
                          :countdown 0
                          :paused? false})
