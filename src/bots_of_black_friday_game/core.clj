(ns bots-of-black-friday-game.core
  (:require [bots-of-black-friday-game.log :as log]
            [bots-of-black-friday-game.behavior :as behavior]
            [bots-of-black-friday-game.behaviors.dummy :as dummy]
            [bots-of-black-friday-game.behaviors.index :as index]
            [bots-of-black-friday-game.visualizer :as vis]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [pelinrakentaja-engine.core :as pr]            )
  (:gen-class))

(def initial-state {:behaviors {}
                    :entities {:data {}
                               :behavioral-entities []
                               :controlled-entities []
                               :system-entities []
                               :entity->types {}
                               :type->entities {}}})

(defn generate-game-state
  [{:keys [system-entities] :as map-data} initial-state]
  (let [state (reduce (fn [state system-entity-id]
                        (let [{:keys [fsm entity effects evaluations]} (get index/id->entity system-entity-id)]
                          (behavior/add-system-entity state entity fsm effects evaluations)))
                      initial-state
                      system-entities)]
    (-> state
        (behavior/add-behavioral-entity dummy/dummy-entity dummy/dummy-fsm dummy/dummy-effects dummy/dummy-evaluations))))

(defn process-map
  "Reads the characters from the string-based map and translates them to weight values"
  [tiles]
  (into {}
    (map-indexed
      (fn [row-index row]
        [row-index
         (into {}
           (map-indexed
             (fn [tile-index character]
               [tile-index
                {:x tile-index
                 :y row-index
                 :weight (case character
                           \x 999
                           \_ 1
                           \# 999
                           0)
                 :tile (case character
                         \x :wall
                         \_ :floor
                         \o :exit
                         \# :trap)}])
             row))])
      tiles)))

(defn load-level
  "Processes level for pathfinding usage
  Level becomes a {y {x value} hash-map
  0, 0 is at the top of the map"
  [level]
  (let [{:keys [map-data] :as loaded-level} (edn/read-string (slurp (io/resource level)))
        processed-map (process-map map-data)
        level-properties (select-keys loaded-level [:title :items :item-limit :system-entities])]
    (merge level-properties
           {:map-data processed-map})))

(defn algo
  [{:keys [bot-name bot-mode] :as _options}]
  (let [bot-name (or bot-name (str (gensym "shodan-"))) ;; generate unique bot name
        current-map (load-level "level.edn")
        game-state (generate-game-state
                    current-map
                    initial-state)]
    (when-not bot-mode
      (vis/load-resources current-map game-state))
    (loop [state game-state
           old-state {}]
      (let [alive? true
            new-state (-> state
                          (behavior/update-system-entities)
                          (behavior/update-behavioral-entities)
                          (behavior/update-controlled-entities))
            new-state (if bot-mode
                        new-state
                        (vis/update-visualizer new-state old-state))]
        (if-not alive?
          {}
          (recur new-state state))))))

(comment
  :game-state {:entities
               {:data {:player {:x 1 :y 2 :fsm {}}
                       :enemy-1 {:x 4 :y 5}
                       :enemy-2 {:x 24 :y 35}}
                :behavioral-entities [:enemy-1 :enemy-2]
                :controlled-entities [:player]
                :entity->types {:enemy-1 :guard
                                :enemy-2 :shopper}
                :type->entities {:guard [:enemy-1]
                                 :shopper [:enemy-2]}}
               :behaviors {}}

  {:require [[:type :guard] [:type :shopper]]
   :id :player
   :pre {:transitions [{:when [::dead ::initialized]
                        :switch :dead}]}
   :current {:state :waiting-for-init
             :effect ::no-op}
   :last {:state nil}
   :states {:dead {:effect ::dead
                   :transitions []}
            :waiting-for-init {:effect ::no-op
                               :transitions [{:when [::instance-found]
                                              :switch :idle
                                              :post-effect ::initialize}]}
            :idle {:effect ::no-op
                   :transitions [{:when [::low-health ::no-potions]
                                  :switch :move-to-exit}
                                 {:when [::low-health ::potions-available]
                                  :switch :move-to-health}
                                 {:when [::affordable-items]
                                  :switch :move-to-item}]}
            :move-to-item {:effect ::move-to-closest-affordable-item
                           :transitions [{:when [::low-health ::potions-available]
                                          :switch :move-to-health}
                                         {:when [::low-health ::no-potions]
                                          :switch :move-to-exit}
                                         {:when [::on-item ::enough-money]
                                          :switch :pick-item}]}
            :move-to-exit {:effect ::move-to-exit
                           :transitions [{:when [::affordable-items]
                                          :switch :move-to-item}]}
            :move-to-health {:effect ::move-to-closest-potion
                             :transitions [{:when [::on-health]
                                            :switch :pick-item}]}
            :pick-item {:effect ::pick-item
                        :transitions [{:when [::item-picked ::no-affordable-items]
                                       :switch :move-to-exit}
                                      {:when [::item-picked ::affordable-items]
                                       :switch :idle}
                                      {:when [::item-picked ::potions-available]
                                       :switch :idle}]}}})

(defn -main
  [& args]
  (prn args)
  (let [options (reduce (fn [acc curr]
                          (cond
                            (= curr "-botmode") (assoc acc :bot-mode true)
                            (= curr "-name") (assoc acc :bot-name nil :set :bot-name)
                            (some? (:set acc)) (assoc acc (:set acc) curr :set nil)
                            :else acc))
                        {}
                        args)
        options (dissoc options :set)]
    (prn options)
  ;; game-info contains the game map

    (if (:bot-mode options)
      (algo options)
      (pr/game-loop
       (algo options)))
    (when-not (:bot-mode options) (vis/load-engine))))
