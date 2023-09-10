(ns bots-of-black-friday-game.map
  (:require [pelinrakentaja-engine.utils.log :as log]))

(defn heuristic
  "Calculates chebyshev distance between nodes start & end"
  [start end]
  (let [{start-x :x start-y :y} start
        {end-x :x end-y :y} end]
    (max (abs (- start-x end-x))
         (abs (- start-y end-y)))))

(defn map-node
  [position map-data]
  (let [{:keys [x y]} position]
    (get-in map-data [y x])))

(defn calculate-weight
  [current from end]
  (try
    (assoc current :route-weight (+ (:weight current)
                                    (:weight from)
                                    (:distance current)
                                    (heuristic current end)))
    (catch Exception e (prn :ex>  current from end))))

(defn same-node?
  [{start-x :x start-y :y :as _current} {end-x :x end-y :y :as _end}]
  (and (<= (abs (- start-x end-x)) 1)
       (<= (abs (- start-y end-y)) 1)))

(defn unvisited-neighbors
  [current visited open end map-data]
  (let [neighbors [(-> current (update :x inc))
                   (-> current (update :x dec))
                   (-> current (update :y inc))
                   (-> current (update :y dec))
                   (-> current (update :x inc) (update :y inc))
                   (-> current (update :x dec) (update :y inc))
                   (-> current (update :x inc) (update :y dec))
                   (-> current (update :x dec) (update :y dec))]
        over-map-removed (into []
                               (remove #(let [{:keys [y x]} %]
                                          (or (>= y (count map-data))
                                              (>= x (count (get map-data 10)))
                                              (< x 0)
                                              (< y 0))))
                               neighbors)
        visited-removed (into []
                              (remove #(let [{:keys [y x]} %]
                                         (some? (get-in visited [y x])))
                                      over-map-removed))
        with-properties (mapv
                         #(-> %
                              (map-node map-data)
                              (assoc :distance (inc (get current :distance 0)))
                              (calculate-weight current end)
                              (assoc :from (select-keys current [:x :y])))
                         visited-removed)]
    (into []
          (concat
           (filter
            (fn [node]
              (nil?
               (some
                #(when (same-node? node %)
                   %)
                with-properties)))
            open)
           with-properties))))

(defn construct-route
  [end visited]
  (reverse
    (loop [route-end end
           route []]
      (let [{{:keys [x y]} :from} route-end]
        (if-not (some? (:from route-end))
          route
          (recur
            (get-in visited [y x])
            (conj route route-end)))))))

(defn find-route
  "A* implementation. Works only for x, y integer type data structures at the moment"
  [start end map-data]
  (log/log :find-route start end)
  (let [open-nodes (unvisited-neighbors start {} [] end map-data)
        sorted-nodes (sort-by :route-weight < open-nodes)
        visited (assoc-in {} [(:y start) (:x start)] (assoc start :from nil))
        [next-node & sorted-nodes] sorted-nodes]
    (loop [open-nodes sorted-nodes
           visited visited
           current-node next-node
           steps 0]
      (let [open-nodes (unvisited-neighbors current-node visited open-nodes end map-data)
            sorted-nodes (sort-by :route-weight < open-nodes)
            {:keys [y x]} current-node]
        (if (or (same-node? current-node end)
                (empty? sorted-nodes))
          (construct-route current-node visited)
          (recur
           (rest sorted-nodes)
           (assoc-in visited [y x] current-node)
           (first sorted-nodes)
           (inc steps)))))))
