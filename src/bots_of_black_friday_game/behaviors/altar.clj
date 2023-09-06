(ns bots-of-black-friday-game.behaviors.altar)

(def altar-effects {::spawn-mammon (fn [self required state])
                    ::convert-money-to-health (fn [self required state])})

(def altar-evaluations {::enough-wealth-to-spawn-mammon? (fn [{:keys [self required] :as state}]
                                                           (> (:wealth self) 10000))
                        ::spawn-complete (fn [state])
                        ::altar-broken (fn [state])})

(def altar-fsm {:require [:clock :mammon]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :gathering-wealth}]}
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
                   :id :generate/altar
                   :type :altar
                   :wealth 0})
