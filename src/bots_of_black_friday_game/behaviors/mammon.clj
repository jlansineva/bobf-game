(ns bots-of-black-friday-game.behaviors.mammon
  (:require [bots-of-black-friday-game.behavior :as behavior]
            [bots-of-black-friday-game.behaviors.fireball :as fireball]
            [bots-of-black-friday-game.behaviors.firewall :as firewall]))

(defn dead? [{:keys [self]}]
  (<= (get self :health) 0))

(defn fireball-cooldown-done? [{:keys [self]}]
  (<= (get-in self [:attacks :fireball :cooldown :counter]) 0))

(defn fireball-attack-max-count? [{:keys [self]}]
  (let [{:keys [max counter]} (get-in self [:attacks :fireball :attacks])]
    (>= counter max)))

(defn fireball-attack-not-max-count? [{:keys [self]}]
  (let [{:keys [max counter]} (get-in self [:attacks :fireball :attacks])]
    (< counter max)))

(defn fireball-attack-cooled-down? [{:keys [self]}]
  (let [{:keys [cooldown rate]} (get-in self [:attacks :fireball :attacks])]
    (> cooldown (/ 1 rate))))

(defn firewall-cooldown-done? [{:keys [self]}]
  (<= (get-in self [:attacks :firewall :cooldown :counter]) 0))

(defn firewall-attack-max-count? [{:keys [self]}]
  (let [{:keys [max counter]} (get-in self [:attacks :firewall :attacks])]
    (>= counter max)))

(defn firewall-attack-not-max-count? [{:keys [self]}]
  (let [{:keys [max counter]} (get-in self [:attacks :firewall :attacks])]
    (< counter max)))

(defn firewall-attack-cooled-down? [{:keys [self]}]
  (let [{:keys [cooldown rate]} (get-in self [:attacks :firewall :attacks])]
    (> cooldown (/ 1 rate))))

(def mammon-evaluations
  {::dead dead?
   ::fireball-cooldown-done fireball-cooldown-done?
   ::fireball-attack-max-count fireball-attack-max-count?
   ::fireball-attack-not-max-count fireball-attack-not-max-count?
   ::fireball-attack-cooled-down fireball-attack-cooled-down?
   ::firewall-cooldown-done firewall-cooldown-done?
   ::firewall-attack-max-count firewall-attack-max-count?
   ::firewall-attack-not-max-count firewall-attack-not-max-count?
   ::firewall-attack-cooled-down firewall-attack-cooled-down?})

(defn dead
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (get-in state [:entities :data self])))

(defn roaming-back-and-forth
  [self {:keys [clock]} state]
  (-> state
      (update-in [:entities :data self :attacks :fireball :cooldown :counter] - (* 1 (:delta-time clock)))
      (update-in [:entities :data self :attacks :firewall :cooldown :counter] - (* 1 (:delta-time clock)))))

(defn fireball-cooldown
  [self {:keys [clock]} state]
  (update-in state [:entities :data self :attacks :fireball :attacks :cooldown] + (* 1 (:delta-time clock))))

(defn do-a-fireball-attack
  [self {:keys [player]} state]
  (let [self-data (get-in state [:entities :data self])
        target player]
    (-> state
        (assoc-in [:entities :data self :attacks :fireball :cooldown :counter] (get-in state [:entities :data self :attacks :fireball :cooldown :max]))
        (update-in [:entities :data self :attacks :fireball :attacks :counter] inc)
        (assoc-in [:entities :data self :attacks :fireball :attacks :cooldown] 0)
        (behavior/add-behavioral-entity (fireball/with-pos-speed-layer
                                         (:position self-data)
                                          (:position target)
                                          :enemy-projectile)
                                        fireball/fireball-fsm
                                        fireball/fireball-effects
                                        fireball/fireball-evaluations))))

(defn firewall-cooldown
  [self {:keys [clock]} state]
  (update-in state [:entities :data self :attacks :firewall :attacks :cooldown] + (* 1 (:delta-time clock))))

(defn do-a-firewall-attack
  [self {:keys [player]} state]
  (let [self-data (get-in state [:entities :data self])
        target player]
    (-> state
        (assoc-in [:entities :data self :attacks :firewall :cooldown :counter] (get-in state [:entities :data self :attacks :firewall :cooldown :max]))
        (update-in [:entities :data self :attacks :firewall :attacks :counter] inc)
        (assoc-in [:entities :data self :attacks :firewall :attacks :cooldown] 0)
        (behavior/add-behavioral-entity (firewall/with-pos-speed-layer
                                         (:position self-data)
                                          (:position target)
                                          :enemy-projectile)
                                        firewall/firewall-fsm
                                        firewall/firewall-effects
                                        firewall/firewall-evaluations))))

(defn reset-weapon-counters
  [self required state]
  (-> state
      (assoc-in [:entities :data self :attacks :fireball :attacks :counter] 0)
      (assoc-in [:entities :data self :attacks :fireball :attacks :cooldown] 0)
      (assoc-in [:entities :data self :attacks :firewall :attacks :counter] 0)
      (assoc-in [:entities :data self :attacks :firewall :attacks :cooldown] 0)))

(defn invincibility-frames-countdown
  [self _ state]
  (let [self-data (get-in state [:entities :data self])]
    (if (> (:invincibility self-data) 0)
      (update-in state [:entities :data self :invincibility] dec)
      state)))

(def mammon-effects
  {::reset-weapon-counters reset-weapon-counters
   ::dead dead
   ::roaming-back-and-forth roaming-back-and-forth
   ::fireball-cooldown fireball-cooldown
   ::do-a-fireball-attack do-a-fireball-attack
   ::firewall-cooldown firewall-cooldown
   ::do-a-firewall-attack do-a-firewall-attack
   ::invincibility-frames-countdown invincibility-frames-countdown})

(defn hurt
  [state entity [damage]]
  (if (<= (:invincibility entity) 0)
    (-> state
        (update-in [:entities :data (:id entity) :health] - damage)
        (assoc-in [:entities :data (:id entity) :invincibility] 5))
    state))

(def mammon-affections
  {:hurt hurt})

(def mammon-fsm
  {:pre {:transitions [{:when [::dead]
                        :switch :dead}]}
   :require [:player :clock [:type :testing]]
   :current {:state :idle :effect :no-op}
   :last {:state nil}
   :states {:idle {:effect :no-op
                   :transitions [{:when [:true]
                                  :switch :roam-back-and-forth}]}
            :dead {:effect ::dead
                   :transitions []}
            :roam-back-and-forth {:effect [::roaming-back-and-forth ::invincibility-frames-countdown]
                                  :transitions [{:when [::fireball-cooldown-done]
                                                 :switch :fireball-attack-mode}
                                                {:when [::firewall-cooldown-done]
                                                 :switch :firewall-attack-mode}]}
            :fireball-attack-mode {:effect [::fireball-cooldown ::invincibility-frames-countdown]
                                   :transitions [{:when [::fireball-attack-max-count]
                                                  :switch :roam-back-and-forth
                                                  :post-effect ::reset-weapon-counters}
                                                 {:when [::fireball-attack-not-max-count ::fireball-attack-cooled-down]
                                                  :switch :do-a-fireball-attack}]}
            :do-a-fireball-attack {:effect ::do-a-fireball-attack
                                   :transitions [{:when [:true]
                                                  :switch :fireball-attack-mode}]}
            :firewall-attack-mode {:effect [::firewall-cooldown ::invincibility-frames-countdown]
                                   :transitions [{:when [::firewall-attack-max-count]
                                                  :switch :roam-back-and-forth
                                                  :post-effect ::reset-weapon-counters}
                                                 {:when [::firewall-attack-not-max-count ::firewall-attack-cooled-down]
                                                  :switch :do-a-firewall-attack}]}
            :do-a-firewall-attack {:effect ::do-a-firewall-attack
                                   :transitions [{:when [:true]
                                                  :switch :firewall-attack-mode}]}}})

(def mammon-entity
  {:id :mammon
   :health 100
   :position {:x 30 :y 30}
   :invincibility 0
   :attacks {:fireball {:cooldown {:counter 5 :max 3}
                        :attacks {:max 5
                                  :rate 2
                                  :cooldown 0
                                  :counter 0}
                        :attacks-per-second 0.5
                        :attack-entity :fireball}
             :firewall {:cooldown {:counter 0 :max 8}
                        :attacks {:max 2
                                  :rate 1
                                  :cooldown 0
                                  :counter 0}
                        :attack-entity :firewall}}
   :type :boss
   :stage 1
   :texture :weapon})
