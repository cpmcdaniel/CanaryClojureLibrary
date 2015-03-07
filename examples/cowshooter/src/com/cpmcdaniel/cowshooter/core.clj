(ns com.cpmcdaniel.cowshooter.core
  (:use net.canarymod.plugin.lang.clojure.clj-plugin)
  (:import (net.canarymod.api.entity.living LivingBase)
           (net.canarymod Canary)
           (net.canarymod.hook.player ItemUseHook)
           (net.canarymod.api.inventory ItemType)
           (net.canarymod.api.world.position Location)
           (net.canarymod.api.entity EntityType)
           (net.canarymod.tasks ServerTask)
           (net.canarymod.api.entity.living.animal Cow)
           (net.canarymod.api.entity.living.humanoid Player)))

(def cow-power (atom 4.0))

(defn fling
  "Launch a living thing from the player on a trajectory"
  [^LivingBase player
   ^LivingBase entity
   ^double factor]
  (let [pitch (double (/ (* (+ (.getPitch player) 90.0) Math/PI) 180.0))
        rot (double (/ (* (+ (.getRotation player) 90.0) Math/PI) 180.0))]
    (.moveEntity entity
                 (* factor (Math/sin pitch) (Math/cos rot))
                 (+ (Math/cos pitch) 0.5)
                 (* factor (Math/sin pitch) (Math/sin rot)))))

(defn spawn-entity-living
  "Spawns a living entity"
  [^Location loc ^EntityType entity-type]
  (doto
    (.. (Canary/factory) (getEntityFactory) (newEntityLiving entity-type loc))
    (.spawn)))


(defn explode-cow [^Cow cow]
  (let [^Location loc (.getLocation cow)]
    (.setHealth cow 0)
    (.kill cow)
    (.. cow (getWorld) (makeExplosion cow
                                      (.getX loc) (.getY loc) (.getZ loc)
                                      @cow-power true))))

(defn burn-cow [^Cow cow]
  (doto cow
    (.setFireTicks 600)
    (.setHealth (.getMaxHealth cow))))

(defn cow-task [^Cow cow plugin]
  (proxy [ServerTask] [(Canary/getServer) 0 true] ; delay = 0, isContinuous
    (run []
      (with-plugin plugin
        (if (.isOnGround cow)
          (do
            (debug "Blowing up cow!")
            (explode-cow cow)
            (.removeSynchronousTask (Canary/getServer) this))
          (burn-cow cow))))))

(defn shoot-cow [^Player player]
  (let [^Location loc (.getLocation player)
        ^Cow cow (spawn-entity-living
                   (doto loc (.setY (inc (.getY loc))))
                   EntityType/MOOSHROOM)]
    (debug "Shooting cow!")
    (.addSynchronousTask (Canary/getServer) (cow-task cow *plugin*))
    (fling player cow 3.0)
    (burn-cow cow)))

(defn shoot-cow-hook [^ItemUseHook hook]
  (let [^Player player (.getPlayer hook)]
    (when (= ItemType/Leather (.. player (getItemHeld) (getType)))
      (shoot-cow player))))

(defn set-cow-power!
  [_ [_ power]]
  (let [pnum (Float/parseFloat power)]
    (debug "Setting cow explosive power")
    (when (< 0 pnum 51)
      (reset! cow-power pnum))))

(defn enable
  "Enable the Cow Shooter Plugin."
  [plugin]
  (info "Enabling cow shooter")
  (register-hook plugin ItemUseHook shoot-cow-hook)
  (register-command plugin {:aliases ["cowpower"]
                            :permissions [""]
                            :description "sets the power of the cow's explosion"
                            :tooltip "/cowpower <power>"
                            :min 2
                            :max 2}
                    set-cow-power!)
  (info "Cow shooter enabled")
  true)

(defn disable
  "Disable the Cow Shooter Plugin."
  [plugin]
  (info "Disabling cow shooter"))

;; (shoot-cow (.. Canary getServer (getPlayer "KowboyMac")))
