(ns bots-of-black-friday-game.behaviors.dummy)

(def dummy-effects {::move-right (fn [self required state]
                                   (let [delta-time (get-in state [:entities :data :clock :delta-time])]
                                     (update-in state [:entities :data self :position :x] + (* 2.0 delta-time))))
                    ::move-down (fn [self required state]
                                  (let [delta-time (get-in state [:entities :data :clock :delta-time])]
                                    (update-in state [:entities :data self :position :y] - (* 2.0 delta-time))))
                    ::move-left (fn [self required state]
                                  (let [delta-time (get-in state [:entities :data :clock :delta-time])]
                                    (update-in state [:entities :data self :position :x] - (* 2.0 delta-time))))
                    ::move-up (fn [self required state]
                                (let [delta-time (get-in state [:entities :data :clock :delta-time])]
                                  (update-in state [:entities :data self :position :y] + (* 2.0 delta-time))))})

(def dummy-evaluations {::close-to-right (fn [{:keys [self required] :as state}]
                                           (> (-> self :position :x) 80))
                        ::close-to-bottom (fn [{:keys [self required] :as state}]
                                            (< (-> self :position :y) 5))
                        ::close-to-left (fn [{:keys [self required] :as state}]
                                          (< (-> self :position :x) 5))
                        ::close-to-top (fn [{:keys [self required] :as state}]
                                         (> (-> self :position :y) 15))})

(def dummy-fsm {:require [:clock]
                :current {:state :idle
                          :effect :no-op}
                :last {:state nil}
                :states {:idle {:effect :no-op
                                :transitions [{:when [:true]
                                               :switch :moving-right}]}
                         :moving-right {:effect ::move-right
                                        :transitions [{:when [::close-to-right]
                                                       :switch :moving-down}]}
                         :moving-down {:effect ::move-down
                                       :transitions [{:when [::close-to-bottom]
                                                      :switch :moving-left}]}
                         :moving-left {:effect ::move-left
                                       :transitions [{:when [::close-to-left]
                                                      :switch :moving-up}]}
                         :moving-up {:effect ::move-up
                                     :transitions [{:when [::close-to-top]
                                                    :switch :moving-right}]}}})

(def dummy-entity {:position {:x 5 :y 15}
                   :id :generate/dummy
                   :type :testing})
