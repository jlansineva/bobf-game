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
(comment
  {:players [],
 :finishedPlayers
 [{:money 1451,
   :name "shodan-2179",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 4608,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 41}
  {:money 4631,
   :name "shodan-204",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 485,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 47}
  {:money 5000,
   :name "shodan-183",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 0,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 28}
  {:money 3650,
   :name "shodan-255",
   :usableItems
   [{:price 110,
     :discountPercent 71,
     :position {:x 47, :y 20},
     :type "WEAPON",
     :isUsable true}],
   :state "MOVE",
   :timeInState 0,
   :score 2550,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 131}
  {:money 2007,
   :name "shodan-157",
   :usableItems
   [{:price 4759,
     :discountPercent 77,
     :position {:x 29, :y 21},
     :type "WEAPON",
     :isUsable true}],
   :state "MOVE",
   :timeInState 0,
   :score 9386,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 246}],
 :items
 [{:price 0,
   :discountPercent 0,
   :position {:x 88, :y 9},
   :type "POTION",
   :isUsable false}
  {:price 0,
   :discountPercent 0,
   :position {:x 9, :y 2},
   :type "POTION",
   :isUsable false}
  {:price 1682,
   :discountPercent 26,
   :position {:x 6, :y 12},
   :type "JUST_SOME_JUNK",
   :isUsable false}
  {:price 1443,
   :discountPercent 64,
   :position {:x 62, :y 6},
   :type "JUST_SOME_JUNK",
   :isUsable false}
  {:price 0,
   :discountPercent 0,
   :position {:x 79, :y 19},
   :type "POTION",
   :isUsable false}],
 :round 169915,
   :shootingLines []})

(comment {:players
 [{:money 3638,
   :name "aleksi",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 4254,
   :health 70,
   :position {:x 47, :y 10},
   :actionCount 94}
  {:money 5000,
   :name "shodan-157",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 0,
   :health 80,
   :position {:x 71, :y 13},
   :actionCount 30}],
 :finishedPlayers
 [{:money 1451,
   :name "shodan-2179",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 4608,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 41}
  {:money 4631,
   :name "shodan-204",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 485,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 47}
  {:money 5000,
   :name "shodan-183",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 0,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 28}
  {:money 3650,
   :name "shodan-255",
   :usableItems
   [{:price 110,
     :discountPercent 71,
     :position {:x 47, :y 20},
     :type "WEAPON",
     :isUsable true}],
   :state "MOVE",
   :timeInState 0,
   :score 2550,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 131}
  {:money 3755,
   :name "shodan-203",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 1682,
   :health 40,
   :position {:x 9, :y 4},
   :actionCount 103}
  {:money 3660,
   :name "shodan-210",
   :usableItems [],
   :state "MOVE",
   :timeInState 0,
   :score 2271,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 139}
  {:money 2007,
   :name "shodan-157",
   :usableItems
   [{:price 4759,
     :discountPercent 77,
     :position {:x 29, :y 21},
     :type "WEAPON",
     :isUsable true}],
   :state "MOVE",
   :timeInState 0,
   :score 9386,
   :health 60,
   :position {:x 9, :y 4},
   :actionCount 246}],
 :items
 [{:price 0,
   :discountPercent 0,
   :position {:x 87, :y 24},
   :type "POTION",
   :isUsable false}
  {:price 0,
   :discountPercent 0,
   :position {:x 37, :y 14},
   :type "POTION",
   :isUsable false}
  {:price 0,
   :discountPercent 0,
   :position {:x 9, :y 2},
   :type "POTION",
   :isUsable false}
  {:price 1922,
   :discountPercent 25,
   :position {:x 79, :y 13},
   :type "JUST_SOME_JUNK",
   :isUsable false}
  {:price 0,
   :discountPercent 0,
   :position {:x 82, :y 5},
   :type "POTION",
   :isUsable false}],
 :round 235646,
 :shootingLines []})

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

(defn get-item-type
  [item-type]
  (case item-type
    "JUST_SOME_JUNK" :item
    "POTION" :potion
    "WEAPON" :weapon))

(defn create-entities
  "Create entities for the engine (logic entities are created elsewhere)"
  [state new-entity-ids]
  (log/log :debug :create-entities new-entity-ids)
  (let [to-add (map #(let [base-entity (get-in state [:entities :data %])
                           renderable-entity
                           (cond-> base-entity
                             (some? (:position base-entity))
                             (assoc :x (get-in base-entity [:position :x])
                                    :y (get-in base-entity [:position :y]))

                             (some? (:position base-entity))
                             (dissoc :position)

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
  [current-map game-state]
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
                            (filter #(and (between 1 (:x %) 90)
                                          (between 1 (:y %) 27)
                                          (some? (#{:wall :exit}
                                                  (:tile %)))))
                            (map #(assoc (select-keys % [:x :y])
                                         :type (:tile %)
                                         :texture {:height 1 :width 1}
                                         :rotation 0
                                         :scale {:x 1 :y 1})))
                           flat-map)]
    (pr/dispatch (vec
                  (concat
                   [:entities/add-entities
                    {:x 2 :y 2 :type :floor :texture {:width 90 :height 24} :rotation 0 :scale {:x 1 :y 1}}
                    {:x 0 :y 0 :type :wall :texture {:width 2 :height 28} :rotation 0 :scale {:x 1 :y 1}}
                    {:x 90 :y 0 :type :wall :texture {:width 2 :height 28} :rotation 0 :scale {:x 1 :y 1}}
                    {:x 0 :y 26 :type :wall :texture {:width 92 :height 2} :rotation 0 :scale {:x 1 :y 1}}
                    {:x 0 :y 0 :type :wall :texture {:width 92 :height 2} :rotation 0 :scale {:x 1 :y 1}}]
                   filtered-map
                   behavioral-entities
                   [{:id :player :x 0 :y 0 :type :player :texture {:width 1 :height 1} :rotation 0 :scale {:x 1 :y 1}}]))))

  (pr/dispatch [:engine/ready]))
