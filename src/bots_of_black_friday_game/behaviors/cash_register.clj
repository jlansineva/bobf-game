(ns bots-of-black-friday-game.behaviors.cash-register)

(def cash-register-effects {::spawn-mammon (fn [self required state])})

(def cash-register-evaluations {::enough-wealth-to-spawn-mammon? (fn [{:keys [self required] :as state}]
                                                           (> (:wealth self) 10000))
                        ::spawn-complete (fn [state])
                        ::altar-broken (fn [state])})

(def cash-register-fsm {:require [:clock :mammon]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :gathering-wealth}]}}})

(def cash-register-entity {:position {:x 40 :y 15}
                     :id :generate/cash-register
                     :type :cash-register
                     :wealth 0})
