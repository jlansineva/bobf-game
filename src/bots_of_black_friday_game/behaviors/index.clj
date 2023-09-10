(ns bots-of-black-friday-game.behaviors.index
  (:require [bots-of-black-friday-game.behaviors.item-spawner :as is]
            [bots-of-black-friday-game.behaviors.item :as i]
            [bots-of-black-friday-game.behaviors.shopper-spawner :as ss]
            [bots-of-black-friday-game.behaviors.shopper :as shopper]
            [pelinrakentaja-engine.dev.tila.clock :as clock]))

(def id->entity {:item-spawner {:fsm is/item-spawner-fsm
                                :entity is/item-spawner-entity
                                :evaluations is/item-spawner-evaluations
                                :effects is/item-spawner-effects}
                 :item {:fsm i/item-fsm
                        :entity i/item-entity
                        :evaluations i/item-evaluations
                        :effects i/item-effects}
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
                                :effects ss/shopper-spawner-effects}})
