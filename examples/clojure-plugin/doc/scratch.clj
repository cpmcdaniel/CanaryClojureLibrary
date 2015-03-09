(ns scratch
  (:use net.canarymod.plugin.lang.clojure.clj-plugin)
  (:import (net.canarymod.api.entity.living LivingBase)
           (net.canarymod Canary LineTracer)
           (net.canarymod.hook.player ItemUseHook)
           (net.canarymod.api.inventory ItemType)
           (net.canarymod.api.world.position Location)
           (net.canarymod.api.entity EntityType Entity)
           (net.canarymod.tasks ServerTask)
           (net.canarymod.api.entity.living.animal Cow)
           (net.canarymod.api.entity.living.humanoid Player)
           (net.canarymod.api.world.blocks BlockType)))

(defn player
  "Gets the player by name"
  [player-name]
  (.. Canary getServer (getPlayer player-name)))

(defn player-loc
  "Gets the location of the given player"
  [player-name]
  (when-let [p (player player-name)]
    (.getLocation p)))

(def kowboy-loc (player-loc "KowboyMac"))


(defn spawn-entity
  "spawns an entity"
  [^Location loc ^EntityType entity-type]
  (doto
      (.. (Canary/factory) (getEntityFactory) (newEntity entity-type loc))
    (.spawn)))



(defprotocol Moveable
  (move [this x y z])
  (movex [this x])
  (movey [this y])
  (movez [this z]))

(extend-protocol Moveable
  Location
  (move [this x y z] (.move x y z))
  (movex [this x] (.moveX x))
  (movey [this y] (.moveY y))
  (movez [this z] (.moveZ z))

  Entity
  (move [this x y z] (.moveEntity this x y z))
  (movex [this x] (.moveEntity this x 0 0))
  (movey [this y] (.moveEntity this 0 y 0))
  (movez [this z] (.moveEntity this 0 0 z)))



(defn line-tracer-seq
  ([from]
   (let [lt (LineTracer. from)]
     (if-let [block (.getNextBlock lt)]
       (line-tracer-seq block lt)
       '())))
  ([block lt]
   (cons block
         (lazy-seq (when-let [block (.getNextBlock lt)]
                     (line-tracer-seq block lt))))))


(defn vertical-shaft
  [block]
  (cons block
        (lazy-seq
         (when (< 0 (.getY block))
           (vertical-shaft (.getRelative block 0 -1 0))))))

(defn tnt-drill
  "Drills a vertical of shaft of TNT ready to be lit"
  [player-or-loc]
  (let [world (.getWorld player-or-loc)]
   (doseq [block (vertical-shaft (first (line-tracer-seq player-or-loc)))]
     (.setBlockAt world (.getLocation block) BlockType/TNT))))


(defn player-seq
  "Gets a sequence of Players currently connected."
  []
  (seq (.. Canary getServer getPlayerList)))


(player-seq)

(move (player "RileyRoo101") 10 10 10)

(tnt-drill (player "KowboyMac"))

(.setFireTicks (player "KowboyMac") 600)
(.setHealth (player "RileyRoo101") 0)

(.setHealth (player "KowboyMac") (.getMaxHealth (player "KowboyMac")))
