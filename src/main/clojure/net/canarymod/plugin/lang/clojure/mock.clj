(ns net.canarymod.plugin.lang.clojure.mock
  (:import (net.canarymod.api.world World DimensionType)
           (net.canarymod.api.world.position Position Location)))

(defn mock-world
  []
  (reify World
    (getType [this] DimensionType/NORMAL)
    (getName [this] "mock")
    (getFqName [this] (str (.getName this) "_" (.getName (.getType this))))
    (toString [this] (.getFqName this))))

;; (Location. (mock-world) 0 0 0 180 0)
