(ns bots-of-black-friday-game.behaviors.item
  (:require [bots-of-black-friday-game.behavior :as behavior]))

(defn queue-for-removal
  [self required state]
  (update-in state [:entities :removal-queue] (comp vec conj) (:id self)))

(def item-effects {::queue-for-removal queue-for-removal })

(defn queued-for-removal
  [{:keys [self required]}]
  (let [to-remove (get required [:entities :removal-queue])]
    (some #(when (= % (:id self)) true) to-remove)))

(defn item-picked
  [{:keys [self]}]
  (:picked? self))

(def item-evaluations {::queued-for-removal queued-for-removal
                       ::item-picked item-picked})

(def item-fsm {:require [[:path [:entities :removal-queue]]]
               :current {:state :on-floor
                         :effect :no-op}
               :last {:state nil}
               :states {:on-floor {:effect :no-op
                                   :transitions [{:when [::item-picked]
                                                  :switch :picked}]}
                        :picked {:effect ::queue-for-removal
                                 :transitions [{:when [::queued-for-removal]
                                                :switch :removed}]}
                        :removed {:effect :no-op
                                  :transitions []}}})

(def item-entity {:position {:x 0 :y 0}
                  :id :generate/item
                  :price 0
                  :discount-percent 0
                  :usable? false
                  :texture :item
                  :type :item})

(comment
;; TODO: maybe you could do something like this to generate stuff automatically
  {:position {:randomize/x [0 80] :randomize/y [0 80]}
          :id :generate/item
          :price 0
          :discount-percent 0
   :usable? false
   :picked? false
          :one-of/texture [:item :weapon]
          :type :item})
