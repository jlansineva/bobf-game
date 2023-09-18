(ns bots-of-black-friday-game.core
  (:require [bots-of-black-friday-game.log :as log]
            [bots-of-black-friday-game.behavior :as behavior]
            [bots-of-black-friday-game.behaviors.dummy :as dummy]
            [bots-of-black-friday-game.behaviors.index :as index]
            [bots-of-black-friday-game.visualizer :as vis]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [pelinrakentaja-engine.core :as pr])
  (:gen-class))

(def initial-state {:behaviors {} ;; TODO: create a level entity
                    :level {:collected-amount 0}
                    :entities {:data {}
                               :removal-queue []
                               :behavioral-entities []
                               :controlled-entities []
                               :system-entities []
                               :entity->types {}
                               :type->entities {}}})

(defn generate-game-state
  [{:keys [system-entities static-entities singleton-entities controlled-entities] :as map-data} initial-state]
  (let [state (reduce (fn [state system-entity-id]
                        (let [{:keys [fsm entity effects evaluations affections]}
                              (get index/id->entity system-entity-id)]
                          (if (some? affections)
                            (behavior/add-system-entity state entity fsm effects evaluations affections)
                            (behavior/add-system-entity state entity fsm effects evaluations))))
                      initial-state
                      system-entities)
        state (reduce (fn [state static-entity-id]
                        (let [{:keys [entity]} (get index/id->entity static-entity-id)
                              entity (assoc entity
                                            :height (count (:map-data map-data))
                                            :width (count (get (:map-data map-data) 0)))]
                          (behavior/add-static-entity state entity)))
                      state
                      static-entities)
        state (reduce (fn [state entity-id]
                        (let [{:keys [fsm entity effects evaluations affections]}
                              (get index/id->entity entity-id)]
                          (if (some? affections)
                            (behavior/add-behavioral-entity state entity fsm effects evaluations affections)
                            (behavior/add-behavioral-entity state entity fsm effects evaluations))))
                      state
                      singleton-entities)
        state (reduce (fn [state entity-id]
                        (let [{:keys [fsm entity effects evaluations affections]}
                              (get index/id->entity entity-id)]
                          (if (some? affections)
                            (behavior/add-controlled-entity state entity fsm effects evaluations affections)
                            (behavior/add-controlled-entity state entity fsm effects evaluations))))
                      state
                      controlled-entities)]
    (-> state
        (behavior/add-behavioral-entity dummy/dummy-entity
                                        dummy/dummy-fsm
                                        dummy/dummy-effects
                                        dummy/dummy-evaluations)
        (assoc :map-data map-data))))

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
        level-properties (select-keys loaded-level [:title :items :item-limit :exit :system-entities :singleton-entities :static-entities :controlled-entities])]
    (merge level-properties
           {:map-data processed-map})))

(defn process-inputs
  [state inputs]
  (let [pressed (into {} (map (fn [[keyval data]] [(get pr/keyval keyval) data]) (inputs)))]
    ;; TODO replace with affections
    (assoc-in state [:entities :data :input] pressed)))

(defn algo
  [{:keys [bot-name bot-mode] :as _options}]
  (let [bot-name (or bot-name (str (gensym "shodan-"))) ;; generate unique bot name
        current-map (load-level "level.edn")
        game-state (generate-game-state
                    current-map
                    initial-state)
        inputs (pr/listen :input/pressed-keys)]
    (when-not bot-mode
      (vis/load-resources current-map game-state))
    (loop [state game-state
           old-state {}]
      (let [alive? true
            new-state (-> state
                          (process-inputs inputs)
                          (behavior/update-system-entities)
                          (behavior/update-behavioral-entities)
                          (behavior/update-controlled-entities)
                          (behavior/remove-dead-entities))
            new-state (if bot-mode
                        new-state
                        (vis/update-visualizer new-state old-state))]
        (if-not alive?
          {}
          (recur new-state state))))))

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
