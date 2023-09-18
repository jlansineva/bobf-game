(ns bots-of-black-friday-game.behaviors.index
  (:require [bots-of-black-friday-game.behaviors.altar :as altar]
            [bots-of-black-friday-game.behaviors.guard-spawner :as gs]
            [bots-of-black-friday-game.behaviors.input :as input]
            [bots-of-black-friday-game.behaviors.item-spawner :as is]
            [bots-of-black-friday-game.behaviors.item :as i]
            [bots-of-black-friday-game.behaviors.level :as level]
            [bots-of-black-friday-game.behaviors.shopper-spawner :as ss]
            [bots-of-black-friday-game.behaviors.shopper :as shopper]
            [bots-of-black-friday-game.behaviors.player :as player]
            [pelinrakentaja-engine.dev.tila.clock :as clock]))

(def id->entity {:item-spawner {:fsm is/item-spawner-fsm
                                :entity is/item-spawner-entity
                                :evaluations is/item-spawner-evaluations
                                :effects is/item-spawner-effects}
                 :item {:fsm i/item-fsm
                        :entity i/item-entity
                        :evaluations i/item-evaluations
                        :effects i/item-effects
                        :affections i/item-affections}
                 :clock {:fsm clock/clock-fsm
                         :entity clock/clock-entity
                         :evaluations clock/clock-evaluations
                         :effects clock/clock-effects}
                 :shopper {:fsm shopper/shopper-fsm
                           :entity shopper/shopper-entity
                           :evaluations shopper/shopper-evaluations
                           :effects shopper/shopper-effects}
                 :shopper-spawner {:fsm ss/shopper-spawner-fsm
                                   :entity ss/shopper-spawner-entity
                                   :evaluations ss/shopper-spawner-evaluations
                                   :effects ss/shopper-spawner-effects}
                  :guard-spawner {:fsm gs/guard-spawner-fsm
                                   :entity gs/guard-spawner-entity
                                   :evaluations gs/guard-spawner-evaluations
                                  :effects gs/guard-spawner-effects}
                 :altar {:fsm altar/altar-fsm
                         :entity altar/altar-entity
                         :evaluations altar/altar-evaluations
                         :effects altar/altar-effects}

                 ;; controlled
                 :player {:fsm player/player-fsm
                          :entity player/player-entity
                          :evaluations player/player-evaluations
                          :effects player/player-effects
                          :affections player/player-affections}

                 ;; static
                 :level {:entity level/level-entity}
                 :input {:entity input/input-entity}})
