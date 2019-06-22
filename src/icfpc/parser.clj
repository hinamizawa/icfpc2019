(ns icfpc.parser
  (:require
   [clojure.string :refer [split]]
   [icfpc.core :refer :all]
   [icfpc.writer :as writer]))

(defn parse-points [points-str]
  (if (not-empty points-str)
    (let [points-seq (split (->> points-str
                                 (drop 1)
                                 (drop-last 1)
                                 (apply str))
                            #"\),\(")]
      (mapv (fn [p]
              (mapv #(Integer/parseInt %) (split p #",")))
            points-seq))
    []))

(defn parse-buster [buster-str]
  (let [[type point-str] (split-at 1 buster-str)]
    [(first type) (first (parse-points point-str))]))

(defn parse-level [task-name]
  (let [description (clojure.string/trim (slurp (str "problems/" task-name)))
        [_ level start obstacles busters] (re-matches #"(.*)#(.*)#(.*)#(.*)" description)]
    {:bot-point (first (parse-points start))
     :corners   (parse-points level)
     :obstacles (mapv parse-points (filter not-empty (split obstacles #";")))
     :boosters  (mapv parse-buster (filter not-empty (split busters #";")))}))

(defn parse-puzzle [puzzle-name]
  (let [description (clojure.string/trim (slurp (str "puzzles/" puzzle-name)))
        [params include exclude] (split description #"#")
        [block-number epoch t-size v-min v-max extra-hands fast-wheels drills teleports cloning spawns] (map #(Integer/parseInt %) (split params #","))]
    {:block-number block-number
     :epoch epoch
     :t-size t-size
     :v-min v-min
     :v-max v-max
     :extra-hands extra-hands
     :fast-wheels fast-wheels
     :drills drills
     :teleports teleports
     :cloning cloning
     :spawns spawns
     :include (parse-points include)
     :exclude (parse-points exclude)}))

(defn validate-puzzle
  "Check if generated map matches puzzle"
  [{:keys [t-size v-min v-max extra-hands fast-wheels drills teleports cloning spawns include exclude] :as puzzle}
   {:keys [width height grid boosters] :as level}]
  (let [min-size (- t-size (Math/floor (* 0.1 t-size)))]
    (when (< (max width height) min-size)
      (throw (Exception. (str "Dimensions are too small: " width "x" height " < " min-size)))))
  (let [area (count (filter #(= EMPTY %) grid))
        min-area (Math/ceil (* 0.2 t-size t-size))]
    (when (< area min-area)
      (throw (Exception. (str "Area is too small: " area " < " min-area)))))
  (let [vertices (count (writer/segments level))]
    (when-not (<= v-min vertices v-max)
      (throw (Exception. (str "Wrong num of vertices: " v-min " <= " vertices " <= " v-max)))))
  (doseq [[booster amount] [[FAST_WHEELS fast-wheels]
                            [DRILL       drills]
                            [TELEPORT    teleports]
                            [CLONE       cloning]
                            [SPAWN       spawns]]
          :let [n (count (filter (fn [[_ b]] (= b booster)) boosters))]]
    (when-not (= n amount)
      (throw (Exception. (str "Wrong amount of booster " booster ": " n " != " amount)))))
  (doseq [[x y] include]
    (when-not (= EMPTY (get-level level x y))
      (throw (Exception. (str "Mistakenly does not include (" x "," y "): " (get-level level x y))))))
  (doseq [[x y] exclude]
    (when-not (= OBSTACLE (get-level level x y))
      (throw (Exception. (str "Mistakenly does include (" x "," y "): " (get-level level x y))))))
  (let [{:keys [x y]} level
        v (get-level level x y)]
    (when-not (= EMPTY v)
      (throw (Exception. (str "Start (" x "," y ") not inside map: " v)))))
  true)

(comment
  (icfpc.parser/validate-puzzle (icfpc.parser/parse-puzzle "puzzle.cond") (icfpc.level/load-level "../puzzles/task.desc"))

)