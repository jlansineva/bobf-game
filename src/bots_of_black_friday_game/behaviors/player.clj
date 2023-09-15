(ns bots-of-black-friday-game.behaviors.player)

(defn moving
  [self {:keys [input clock]} state]
  (let [self-data (get-in state [:entities :data self])]
    (cond-> state
      (get-in input [:keys :left :pressed?])
      (update-in [:entities :data self :position :x] - (* (:speed self-data) (:delta-time clock)))

      (get-in input [:keys :right :pressed?])
      (update-in [:entities :data self :position :x] + (* (:speed self-data) (:delta-time clock)))

      (get-in input [:keys :up :pressed?])
      (update-in [:entities :data self :position :y] - (* (:speed self-data) (:delta-time clock)))

      (get-in input [:keys :down :pressed?])
      (update-in [:entities :data self :position :y] + (* (:speed self-data) (:delta-time clock))))))

(defn shooting
  [self {:keys [input]} state]
  (prn :> :shooting)
  state)

(def player-effects
  {::moving moving
   ::shooting shooting})

(defn moving?
  [{:keys [required]}]
  (let [{:keys [input]} required]
    (or
      (true? (get-in input [:keys :left :pressed?]))
      (true? (get-in input [:keys :right :pressed?]))
      (true? (get-in input [:keys :up :pressed?]))
      (true? (get-in input [:keys :down :pressed?])))))

(defn stopped?
  [state]
  (not (moving? state)))

(def player-evaluations
  {::moving moving?})

(def player-fsm
  {:require [:input :clock]
   :current {:state :idle :effect :no-op}
   :last {:state nil}
   :states {:idle {:effect :no-op
                   :transitions [{:when [::moving]
                                  :switch :moving}]}
            :moving {:effect [::moving ::shooting]
                     :transitions [{:when [::stopped]
                                    :switch :idle}]}}})

(def player-entity
  {:id :player
   :type :player
   :texture :player
   :speed 6
   :position {:x 15 :y 15}})
