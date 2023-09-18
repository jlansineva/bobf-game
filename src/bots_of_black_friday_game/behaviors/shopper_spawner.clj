(ns bots-of-black-friday-game.behaviors.shopper-spawner
  (:require [bots-of-black-friday-game.behaviors.shopper :as shopper]
            [bots-of-black-friday-game.behavior :as behavior]))

(defn countdown-for-spawn
  [self required state]
  (let [{:keys [clock]} required
        countdown-decrease (* (:delta-time clock) 1.0)]
    (update-in state
               [:entities :data self :countdown]
               - countdown-decrease)))

(defn spawn-a-shopper
  [self {:keys [level] :as required} state]
  (prn :> :spawning-a-shopper required)
  (let [countdown-max (get-in state [:entities :data self :max-countdown])
        countdown-reset (assoc-in state [:entities :data self :countdown] countdown-max)]
    (behavior/add-behavioral-entity
     countdown-reset
     (-> shopper/shopper-entity
         (update :position
                 assoc
                 :x (if (< (rand-int 10) 5) 4 (- (:height level) 4))
                 :y (+ 2 (rand-int (- (:height level) 4)))))
     shopper/shopper-fsm
     shopper/shopper-effects
     shopper/shopper-evaluations)))

(def shopper-spawner-effects {::countdown-for-spawn countdown-for-spawn
                              ::spawn-a-shopper spawn-a-shopper})

(defn clock-paused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:paused? clock)))

(defn spawn-countdown-zero
  [{:keys [self required]}]
  (<= (:countdown self) 0))

(defn under-spawned-shopper-limit
  [{:keys [self required]}]
  (let [{:keys [shopper level]} required]
    (< (count (keys shopper)) (:max-shoppers self))))

(defn clock-unpaused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:unpaused? clock)))

(defn boss-stage
  [{:keys [self required]}]
  (let [{:keys [level]} required]
                                                ;; TODO: once we figure out how levels work
    ))

(def shopper-spawner-evaluations {::clock-paused clock-paused
                                  ::spawn-countdown-zero spawn-countdown-zero
                                  ::under-spawned-shopper-limit under-spawned-shopper-limit
                                  ::clock-unpaused clock-unpaused
                                  ::boss-stage boss-stage})

(def shopper-spawner-fsm {:require [:clock :level [:type :shopper]]
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
                                                                   {:when [::spawn-countdown-zero ::under-spawned-shopper-limit]
                                                                    :switch :spawning}]}
                                   :spawning {:effect ::spawn-a-shopper
                                              :transitions [{:when [:true]
                                                             :switch :spawner-running}]}
                                   :paused {:effect :no-op
                                            :transitions [{:when [::clock-unpaused]
                                                           :switch :spawner-running}]}
                                   :stopped {:effect :no-op
                                             :transitions []}}})

(def shopper-spawner-entity {:position {:x 40 :y 15}
                             :id :shopper-spawner
                             :type :shopper-spawner
                             :countdown 0
                             :max-countdown 10
                             :max-shoppers 15
                             :paused? false})
