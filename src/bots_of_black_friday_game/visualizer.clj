(ns bots-of-black-friday-game.visualizer
  (:require [clojure.string :as str]
            [pelinrakentaja-engine.core :as pr]
            [pelinrakentaja-engine.utils.log :as log]))

(defonce engine nil)

(defn load-engine
  []
  (reset! engine (pr/initialize-window {:title "Bots of Black Friday"})))

(defn between [lower compare upper]
  (and (> compare lower) (< compare upper)))

(defn id-for-item
  [item]
  (let [item-id (keyword (str (-> item :position :x) "-" (-> item :position :y) "-" (:type item)))]
    #_(log/log :debug :id-for-item item item-id)
    item-id))

(defn diff-ids
  [state old-state id-fn]
  (let [old-ids (into #{} (map id-fn) old-state)
        new-ids (into #{} (map id-fn) state)
        to-remove (into [] (filter #(not (% new-ids)) old-ids))
        to-add (into [] (filter #(not (% old-ids)) new-ids))]
    (log/log :debug :game/update-items old-ids :-> new-ids :remove> to-remove :add> to-add)
    {:to-add to-add :to-remove to-remove}))

(defn collect-diffs
  [collected to-add to-remove items id-fn]
  (-> collected
        (update :to-remove #(vec (concat % to-remove)))
        (update :to-add (fn [add]
                          (vec (concat
                                add
                                (filter (fn [item]
                                          (some #(when (= %
                                                          (id-fn item))
                                                   true)
                                                to-add))
                                        items)))))))

(defn update-behavioral-entities
  [collected state old-state]
  (let [behavioral-entity-ids (get-in state [:entities :behavioral-entities])
        old-behavioral-entity-ids (get-in old-state [:entities :behavioral-entities])
        {:keys [to-add to-remove]} (diff-ids behavioral-entity-ids old-behavioral-entity-ids identity)]
    (log/log :debug :game/update-items collected)
    (collect-diffs collected to-add to-remove behavioral-entity-ids identity))) ;; TODO simplify

(defn player-id
  [player]
  (keyword (str/replace (:name player) #" " "-")))

(defn update-players
  [collected players old-players local-player]
  (let [players (filter #(not (= (:name %) (:name local-player))) players)
        old-players (filter #(not (= (:name %) (:name local-player))) old-players)
        {:keys [to-add to-remove]} (diff-ids players old-players player-id)]
    (collect-diffs collected to-add to-remove players player-id)))

(defn get-x-y-from-position
  [entity]
  (-> entity
      (assoc :x (get-in entity [:position :x])
             :y (get-in entity [:position :y]))
      (dissoc :position)))

(defn use-texture-key
  [entity]
  (-> entity
      (assoc :type (:texture entity))
      (dissoc :texture)))

(defn create-entities
  "Create entities for the engine (logic entities are created elsewhere)"
  [state new-entity-ids]
  (log/log :debug :create-entities new-entity-ids)
  (let [to-add (map #(let [base-entity (get-in state [:entities :data %])
                           renderable-entity
                           (cond-> base-entity
                             (some? (:position base-entity))
                             get-x-y-from-position

                             (some? (:texture base-entity))
                             use-texture-key

                             true (assoc :texture {:width 1 :height 1}
                                         :rotation 0
                                         :scale {:x 1 :y 1}))]
                       renderable-entity)
                    new-entity-ids)]
    to-add))

(defn create-entity-update-events
  [entity-data entities]
;  (prn :> entity-data)
;  (prn :> entities)
  (mapv
   #(let [{:keys [id position]} (get entity-data %)]
      [id position [:x :y]])
   entities))

(defn update-visualizer
  [state old-state]
  (let [{:keys [to-add to-remove]}
        (-> {}
            (update-behavioral-entities state old-state)
            #_(update-controlled-entities state old-state))]
    (pr/dispatch [:audio/play-music :level-music])
    (pr/update!)
    (log/log :debug :game/update-items :remove> to-remove :add> to-add)
    (when (seq to-add)
      (pr/dispatch (into [:entities/add-entities] (create-entities state to-add))))
    (when (seq to-remove)
      (pr/dispatch (into [:entities/remove-entities-with-ids] to-remove)))
    (let [updateable-entities (create-entity-update-events (get-in state [:entities :data]) (get-in state [:entities :behavioral-entities]))]
      (pr/dispatch (into [:entities/update-entities-id-properties]
                         updateable-entities)))
    state))

(defn load-resources
  [current-map game-state] ;; TODO: maybe refactor type to something else -> resource id e.g.
  (pr/dispatch [:resources/load-texture {:type :wall :texture "bricks.png"}])
  (pr/dispatch [:resources/load-texture {:type :exit :texture "exit.png"}])
  (pr/dispatch [:resources/load-texture {:type :floor :texture "floor.png"}])
  (pr/dispatch [:resources/load-texture {:type :player :texture "player.png"}])
  (pr/dispatch [:resources/load-texture {:type :enemy :texture "enemy.png"}])
  (pr/dispatch [:resources/load-texture {:type :weapon :texture "weapon.png"}])
  (pr/dispatch [:resources/load-texture {:type :item :texture "present.png"}])
  (pr/dispatch [:resources/load-texture {:type :testing :texture "present.png"}])
  (pr/dispatch [:resources/load-texture {:type :potion :texture "tuoppi.png"}])
  (pr/dispatch [:resources/load-resource {:type :level-music :music "drums.mp3"}])

  (let [flat-map (mapcat (comp vector second)
                         (mapcat second (:map-data current-map)))
        behavioral-entities (create-entities game-state (get-in game-state [:entities :behavioral-entities]))
        filtered-map (into []
                           (comp
                            (filter #(some? (#{:wall :exit}
                                             (:tile %))))
                            (map #(assoc (select-keys % [:x :y])
                                         :type (:tile %)
                                         :texture {:height 1 :width 1}
                                         :rotation 0
                                         :scale {:x 1 :y 1})))
                           flat-map)]
    (pr/dispatch (vec
                  (concat
                   [:entities/add-entities
                    {:x 2 :y 2 :type :floor :texture {:width 90 :height 24} :rotation 0 :scale {:x 1 :y 1}}]
                   filtered-map
                   behavioral-entities
                   [{:id :player :x 0 :y 0 :type :player :texture {:width 1 :height 1} :rotation 0 :scale {:x 1 :y 1}}]))))

  (pr/dispatch [:engine/ready]))
