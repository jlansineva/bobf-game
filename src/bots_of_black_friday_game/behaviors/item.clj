(ns bots-of-black-friday-game.behaviors.item)

(def item-effects {::spawn-mammon (fn [self required state])})

(def item-evaluations {::enough-wealth-to-spawn-mammon? (fn [{:keys [self required] :as state}]
                                                           (> (:wealth self) 10000))
                        ::spawn-complete (fn [state])
                        ::altar-broken (fn [state])})

(def item-fsm {:require [:clock :mammon]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :gathering-wealth}]}}})

(def item-entity {:position {:x 40 :y 15}
                     :id :generate/item
                     :type :cultist
                     :wealth 0})
