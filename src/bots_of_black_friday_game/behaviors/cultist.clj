(ns bots-of-black-friday-game.behaviors.cultist)

(def cultist-effects {::spawn-mammon (fn [self required state])})

(def cultist-evaluations {::enough-wealth-to-spawn-mammon? (fn [{:keys [self required] :as state}]
                                                           (> (:wealth self) 10000))
                        ::spawn-complete (fn [state])
                        ::altar-broken (fn [state])})

(def cultist-fsm {:require [:clock :mammon]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :gathering-wealth}]}}})

(def cultist-entity {:position {:x 40 :y 15}
                     :id :generate/cultist
                     :type :cultist
                     :wealth 0})
