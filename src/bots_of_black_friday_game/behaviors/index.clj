(ns bots-of-black-friday-game.behaviors.index
  (:require [bots-of-black-friday-game.behaviors.item-spawner :as is]
            [bots-of-black-friday-game.behaviors.item :as i]
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
                         :effects clock/clock-effects}})
