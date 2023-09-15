(ns bots-of-black-friday-game.behaviors.guard-spawner
  (:require [bots-of-black-friday-game.behaviors.guard :as guard]
            [bots-of-black-friday-game.behavior :as behavior]))

(defn countdown-for-spawn
  [self required state]
  (let [{:keys [clock]} required
        countdown-decrease (* (:delta-time clock) 1.0)]
    (update-in state
               [:entities :data self :countdown]
               - countdown-decrease)))

(defn spawn-a-guard
  [self required state]
  (prn :> :spawning-a-guard required)
  (let [countdown-max (get-in state [:entities :data self :max-countdown])
        countdown-reset (assoc-in state [:entities :data self :countdown] countdown-max)]
    (behavior/add-behavioral-entity
     countdown-reset
     (-> guard/guard-entity
         (update :position assoc :x 4 :y (rand-int 20)))
     guard/guard-fsm
     guard/guard-effects
     guard/guard-evaluations)))

(def guard-spawner-effects {::countdown-for-spawn countdown-for-spawn
                              ::spawn-a-guard spawn-a-guard})

(defn clock-paused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:paused? clock)))

(defn spawn-countdown-zero
  [{:keys [self required]}]
  (<= (:countdown self) 0))

(defn under-spawned-guard-limit
  [{:keys [self required]}]
  (let [{:keys [guard level]} required]
    (< (count (keys guard)) (:max-guards self))))

(defn clock-unpaused
  [{:keys [self required]}]
  (let [{:keys [clock]} required]
    (:unpaused? clock)))

(defn boss-stage
  [{:keys [self required]}]
  (let [{:keys [level]} required]
                                                ;; TODO: once we figure out how levels work
    ))

(def guard-spawner-evaluations {::clock-paused clock-paused
                                  ::spawn-countdown-zero spawn-countdown-zero
                                  ::under-spawned-guard-limit under-spawned-guard-limit
                                  ::clock-unpaused clock-unpaused
                                  ::boss-stage boss-stage})

(def guard-spawner-fsm {:require [:clock :level [:type :guard]]
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
                                                                   {:when [::spawn-countdown-zero ::under-spawned-guard-limit]
                                                                    :switch :spawning}]}
                                   :spawning {:effect ::spawn-a-guard
                                              :transitions [{:when [:true]
                                                             :switch :spawner-running}]}
                                   :paused {:effect :no-op
                                            :transitions [{:when [::clock-unpaused]
                                                           :switch :spawner-running}]}
                                   :stopped {:effect :no-op
                                             :transitions []}}})

(def guard-spawner-entity {:position {:x 40 :y 15}
                             :id :guard-spawner
                             :type :guard-spawner
                             :countdown 0
                             :max-countdown 20
                             :max-guards 4
                             :paused? false})
