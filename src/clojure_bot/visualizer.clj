(ns clojure-bot.visualizer
  (:require [pelinrakentaja-engine.core :as pr]
            [pelinrakentaja-engine.utils.log :as log]))

(defonce engine nil)

(defn load-engine
  []
  (reset! engine (pr/initialize-window "Bots of Black Friday")))

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

(defn id-for-item
  [item]
  (let [item-id (keyword (str (-> item :position :x) "-" (-> item :position :y) "-" (:type item)))]
    (log/log :debug :id-for-item item item-id)
    item-id))

(defn update-items
  [collected items old-items]
  (let [old-ids (into #{} (map id-for-item) old-items)
        new-ids (into #{} (map id-for-item) items)
        to-remove (into [] (filter #(not (% new-ids)) old-ids))
        to-add (into [] (filter #(not (% old-ids)) new-ids))]
    (log/log :debug :update-items collected)
    (log/log :debug :update-items old-ids :-> new-ids :remove> to-remove :add> to-add)
    (-> collected
        (update :remove #(vec (concat % to-remove)))
        (update :add (fn [add]
                       (vec (concat
                             add
                             (filter (fn [item]
                                       (some #(when (= (:id %)
                                                       (id-for-item item))
                                                true)
                                             to-add))
                                     items)))))))) ;; TODO simplify

(defn update-players
  [])

(defn get-item-type
  [item-type]
  (case item-type
    "JUST_SOME_JUNK" :item
    "POTION" :potion
    "WEAPON" :weapon))

(defn create-entities
  [new-entities]
  (map #(-> {}
            (merge (:position %))
            (assoc :texture {:width 1 :height 1}
                   :rotation 0
                   :scale {:x 1 :y 1})
            (assoc :id (id-for-item %)
                   :type (get-item-type (:type %)))) new-entities))

(defn update-visualizer
  [state old-state]
  (let [{:keys [add remove]} (-> {}
                                 (update-items (:items state) (:items old-state)))]
    (when (seq add) (pr/dispatch (into [:entities/add-entities] (create-entities add))))
    #_(when (seq remove)
      (pr/dispatch (into [:entities/remove-entities-with-id] remove)))
    (pr/dispatch [:entities/update-entities-id-properties
                  [:player (get-in state [:player :local :position]) [:x :y]]])
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
  (pr/dispatch [:resources/load-texture {:type :potion :texture "tuoppi.png"}])

  (let [flat-map (mapcat (comp vector second)
                         (mapcat second current-map))
        items (create-entities (:items game-state))
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
                   items
                   [{:id :player :x 0 :y 0 :type :player :texture {:width 1 :height 1} :rotation 0 :scale {:x 1 :y 1}}]))))

  (pr/dispatch [:engine/ready]))
