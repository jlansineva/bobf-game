(ns bots-of-black-friday-game.behaviors.utils)

(defn get-movement-vector
  [pos-a pos-b movement-speed]
  (let [dx (- (:x pos-b) (:x pos-a))
        dy (- (:y pos-b) (:y pos-a))
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (<= dist movement-speed)
      {:x dx :y dy}
      {:x (* (/ dx dist) movement-speed)
       :y (* (/ dy dist) movement-speed)})))

(defn get-distance
  [pos-a pos-b]
  (let [dx (- (:x pos-b) (:x pos-a))
        dy (- (:y pos-b) (:y pos-a))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))
