(ns bots-of-black-friday-game.behaviors.altar
  (:require [bots-of-black-friday-game.behaviors.mammon :as mammon]
            [bots-of-black-friday-game.behavior :as behavior]))

(defn spawn-mammon
  [self required state]
  (prn :boss-stage>)
  (behavior/add-behavioral-entity
    state
    mammon/mammon-entity
    mammon/mammon-fsm
    mammon/mammon-effects
    mammon/mammon-evaluations
    mammon/mammon-affections))

(defn convert-money-to-wealth
  [self required state]
  (let [{{:keys [money-collected]} :level} required]
    (if (> money-collected 0)
      (do
        (prn :> :converting money-collected (get-in state [:entities :data self :wealth]))
        (-> state
            (update-in [:entities :data self :wealth] + money-collected)
            (assoc-in [:entities :data :level :money-collected] 0)))
      state)))

(defn convert-money-to-health
  [])

(def altar-effects {::spawn-mammon spawn-mammon
                    ::convert-money-to-wealth convert-money-to-wealth
                    ::generate-energy (fn [self required state]
                                        (prn :genergy)
                                        state)
                    ::protect-mammon (fn [self required state]
                                       (prn :>protecto)
                                       state)})

(defn enough-wealth-to-spawn-mammon?
  [{:keys [self required] :as state}]
  (> (:wealth self) 1000))

(def altar-evaluations {::enough-wealth-to-spawn-mammon? enough-wealth-to-spawn-mammon?
                        ::spawn-complete (fn [state] true)
                        ::altar-broken (fn [state] true)})

(def altar-fsm {:require [:clock :level :mammon]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :gathering-wealth}]}
                         :dead {:effect :no-op
                                :transitions []}
                         :gathering-wealth {:effect ::convert-money-to-wealth
                                            :transitions [{:when [::enough-wealth-to-spawn-mammon?]
                                                           :switch :spawning-mammon}]}
                         :spawning-mammon {:effect ::generate-energy
                                           :transitions [{:when [::spawn-complete]
                                                          :switch :sustaining-mammon
                                                          :post-effect ::spawn-mammon}]}
                         :sustaining-mammon {:effect ::protect-mammon
                                             :transitions [{:when [::altar-broken]
                                                            :switch :dead}]}}})

(comment [[:when :clock :elapsed-time :is :greater-than 1000 :spawn-mammon]])

(def altar-entity {:position {:x 40 :y 15}
                   :id :altar
                   :type :altar
                   :wealth 0})
