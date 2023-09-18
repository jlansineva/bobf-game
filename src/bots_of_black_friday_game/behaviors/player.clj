(ns bots-of-black-friday-game.behaviors.player
  (:require
   [bots-of-black-friday-game.behavior :as behavior]
   [bots-of-black-friday-game.behaviors.utils :as utils]
    [bots-of-black-friday-game.behaviors.fireball :as fireball]))

(defn moving
  [self {:keys [input clock]} state]
  (let [self-data (get-in state [:entities :data self])]
    (cond-> state
      (get-in input [:left :pressed?])
      (update-in [:entities :data self :position :x] - (* (:speed self-data) (:delta-time clock)))

      (get-in input [:right :pressed?])
      (update-in [:entities :data self :position :x] + (* (:speed self-data) (:delta-time clock)))

      (get-in input [:up :pressed?])
      (update-in [:entities :data self :position :y] - (* (:speed self-data) (:delta-time clock)))

      (get-in input [:down :pressed?])
      (update-in [:entities :data self :position :y] + (* (:speed self-data) (:delta-time clock))))))

(defn shooting
  [self {:keys [input clock]} state]
  (let [self-attacks (get-in state [:entities :data self :attacks])
        self-pos (get-in state [:entities :data self :position])
        direction (cond-> {}
                    (get-in input [:left :pressed?]) (assoc :x -1.0)
                    (get-in input [:right :pressed?]) (assoc :x 1.0)
                    (get-in input [:up :pressed?]) (assoc :y -1.0)
                    (get-in input [:down :pressed?]) (assoc :y 1.0))
        direction (cond
                    (and (empty? (keys direction))
                         (some? (:last-direction self-attacks)))
                    (:last-direction self-attacks)

                    (and (= self-pos direction)
                         (nil? (:last-direction self-attacks)))
                    (assoc direction :x 1.0)

                    :else
                    direction)]
    (if (<= (:cooldown self-attacks) 0)
      (-> state
          (assoc-in [:entities :data self :attacks :last-direction] direction)
          (assoc-in [:entities :data self :attacks :cooldown] (:max self-attacks))
          (behavior/add-behavioral-entity (fireball/with-pos-speed-layer
                                            self-pos
                                            (-> self-pos (update :x + (get direction :x 0)) (update :y + (get direction :y 0)))
                                            :player-projectile)
                                          fireball/fireball-fsm
                                          fireball/fireball-effects
                                          fireball/fireball-evaluations))
      (-> state
          (assoc-in [:entities :data self :attacks :last-direction] direction)
          (update-in [:entities :data self :attacks :cooldown] - (:delta-time clock))))))

(defn pick-item-at-current-position
  [self {:keys [item]} state]
  (let [items (vals item)
        self-data (get-in state [:entities :data self])
        item (some (fn [item] (when (< (utils/get-distance (:position self-data) (:position item)) 2) item))
                   items)
        {:keys [affect]} item]
    (-> state
        (update-in [:entities :data self :speed] inc)
        (affect [:pickup]))))

(defn invincibility
  [self required state]
  (let [{:keys [invincibility health]} (get-in state [:entities :data self])]
    (if (> invincibility 0)
      (update-in state [:entities :data self :invincibility] dec)
      state)))

(def player-effects
  {::moving moving
   ::shooting shooting
   ::invincibility invincibility
   ::pick-item-at-current-position pick-item-at-current-position})

(defn moving?
  [{:keys [required]}]
  (let [{:keys [input]} required]
    (or
      (true? (get-in input [:left :pressed?]))
      (true? (get-in input [:right :pressed?]))
      (true? (get-in input [:up :pressed?]))
      (true? (get-in input [:down :pressed?])))))

(defn stopped?
  [state]
  (not (moving? state)))

(defn dead?
  [{:keys [self]}]
  (<= (:health self) 0))

(defn at-powerup?
  [{:keys [self required]}]
  (let [{:keys [item]} required
        items (vals item)]
    (some? (some (fn [item] (when (< (utils/get-distance (:position self) (:position item)) 2) item))
                 items))))

(def player-evaluations
  {::moving moving?
   ::stopped stopped?
   ::dead dead?
   ::at-powerup at-powerup?})

(defn hurt
  [state entity [damage]]
  (if (<= (:invincibility entity) 0)
    (-> state
        (update-in [:entities :data (:id entity) :health] - damage)
        (assoc-in [:entities :data (:id entity) :invincibility] 5))
    state))

(def player-affections
  {:hurt hurt})

(def player-fsm
  {:pre {:transitions [{:when [::dead]
                        :switch :dead}]}
   :require [:input :clock [:layer :enemy-projectile] [:type :item]]
   :current {:state :idle :effect :no-op}
   :last {:state nil}
   :states {:dead {:effect :no-op
                   :transitions []}
            :idle {:effect [::shooting ::invincibility]
                   :transitions [{:when [::moving]
                                  :switch :moving}]}
            :moving {:effect [::moving ::shooting ::invincibility]
                     :transitions [{:when [::stopped]
                                    :switch :idle}
                                   {:when [::at-powerup]
                                    :switch :moving
                                    :post-effect ::pick-item-at-current-position}]}}})

(def player-entity
  {:id :player
   :type :player
   :texture :player
   :invincibility 0
   :speed 6
   :health 100
   :attacks {:cooldown 0 :max 1}
   :position {:x 15 :y 15}})
