(ns bots-of-black-friday-game.behavior
  (:require [pelinrakentaja-engine.dev.tila :as tila]))

(comment
  {:behaviors {}
   :entities
          {:data {:player {:x 1 :y 2 :fsm {}}
                  :enemy-1 {:x 4 :y 5}
                  :enemy-2 {:x 24 :y 35}
                  :clock {:current-millis 0 :last-millis 0 :delta-time 0 :elapsed-time 0
                          :started? true
                          :paused? false
                          :unpause? false
                          :stop? false}}
           :behavioral-entities [:enemy-1 :enemy-2]
           :controlled-entities [:player]
           :system-entities [:clock]
           :entity->types {:enemy-1 :guard
                           :enemy-2 :shopper}
           :type->entities {:guard [:enemy-1]
                            :shopper [:enemy-2]}}})

(defn update-entity
  [state entity-id]
  (let [{:keys [update apply-effect] :as entity-behavior} (get-in state [:behaviors entity-id])
        {:keys [fsm state]} (update entity-behavior state)]
    (assoc-in (apply-effect fsm state) [:behaviors entity-id] fsm)))

(defn update-controlled-entities
  [state]
  (let [controlled-entities (get-in state [:entities :controlled-entities])]
    (reduce update-entity state controlled-entities)))

(defn update-behavioral-entities
  [state]
  (let [behavioral-entities (get-in state [:entities :behavioral-entities])]
    (reduce update-entity state behavioral-entities)))

(defn update-system-entities
  [state]
  (let [system-entities (get-in state [:entities :system-entities])]
    (reduce update-entity state system-entities)))

(defn generate-id
  [id]
  (let [generate? (= :generate (-> id namespace keyword))]
    (if generate?
      (keyword (gensym (name id)))
      id)))

(defn add-controlled-entity
  [])

(defn add-behavioral-entity
  [state entity logic effects evaluations]
  {:pre [(some? (:id entity))
         (some? (:type entity))]}
  (let [id (generate-id (:id entity))
        entity (assoc entity :id id)
        logic-linked-to-id (assoc logic :id id)
        logic-fsm (tila/register-behavior entity logic-linked-to-id effects evaluations)]
    (prn :abe> id)
    (-> state
        (assoc-in [:behaviors id] logic-fsm)
        (assoc-in [:entities :data id] entity)
        (update-in [:entities :behavioral-entities] (comp vec conj) id)
        (assoc-in [:entities :entity->types id] (:type entity))
        (assoc-in [:entities :type->entities (:type entity)] id))))

(defn add-system-entity
  [state entity logic effects evaluations]
  {:pre [(some? (:id entity))]}
  (let [id (generate-id (:id entity))
        entity (assoc entity :id id)
        logic-linked-to-id (assoc logic :id id)
        logic-fsm (tila/register-behavior entity logic-linked-to-id effects evaluations)]
    (prn :ase> id)
    (-> state
        (assoc-in [:behaviors id] logic-fsm)
        (assoc-in [:entities :data id] entity)
        (update-in [:entities :system-entities] (comp vec conj) id))))

(defn add-static-entity
  [state entity])