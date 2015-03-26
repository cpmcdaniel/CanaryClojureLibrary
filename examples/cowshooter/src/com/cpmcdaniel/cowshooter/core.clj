(ns com.cpmcdaniel.cowshooter.core
  (:require [net.canarymod.plugin.lang.clojure.canary :refer :all])
  (:import (net.canarymod Canary)
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
  [source projectile factor]
  (let [pitch (double (/ (* (+ (pitch source) 90.0) Math/PI) 180.0))
        rot (double (/ (* (+ (rotation source) 90.0) Math/PI) 180.0))]
    (move projectile
          (* factor (Math/sin pitch) (Math/cos rot))
          (+ (Math/cos pitch) 0.5)
          (* factor (Math/sin pitch) (Math/sin rot)))))

(defn burn-cow [cow]
  (doto cow
    (burn 600)
    (max-health!)))

(defn cow-task [^Cow cow plugin]
  (proxy [ServerTask] [(Canary/getServer) 0 true] ; delay = 0, isContinuous
    (run []
      (with-plugin plugin
        (if (.isOnGround cow)
          (do
            (debug "Blowing up cow!")
            (explode cow @cow-power)
            (.removeSynchronousTask (Canary/getServer) this))
          (burn-cow cow))))))

(defn shoot-cow [^Player player]
  (let [^Location loc (.getLocation player)
        ^Cow cow (spawn-entity
                  (move-y loc 1)
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

;;(shoot-cow (.. Canary getServer (getPlayer "KowboyMac")))
