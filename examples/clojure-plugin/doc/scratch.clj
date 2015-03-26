(ns scratch
  (:require [net.canarymod.plugin.lang.clojure.canary :refer :all]
            [clojure.pprint :refer [pprint]])
  (:import (net.canarymod.api.entity.living LivingBase)
           (net.canarymod Canary LineTracer)
           (net.canarymod.hook.player ItemUseHook)
           (net.canarymod.api.inventory ItemType)
           (net.canarymod.api.world.position Location)
           (net.canarymod.api.entity EntityType Entity)
           (net.canarymod.tasks ServerTask)
           (net.canarymod.api.entity.living.animal Cow)
           (net.canarymod.api.entity.living.humanoid Player)
           (net.canarymod.api.world.blocks BlockType)
           (net.canarymod.chat ChatFormat)
           (java.util Random)))



(.broadcastMessage (Canary/getServer)
                   (str ChatFormat/AQUA "Hello from the REPL"))


(reload-plugin)

(register-command *plugin* {:aliases ["hello"]
                            :permissions [""]
                            :description "say hello"
                            :tooltip "/hello <playername> <message>"
                            :min 3
                            :max 3}
                  (fn [sender [_ player-name message]]
                    (.message (player player-name) message)))


(defn my-hook [hook]
  (let [p (.getPlayer hook)]
    (when (= ItemType/Leather (.. p (getItemHeld) (getType)))
        (.broadcastMessage (Canary/getServer) "my leather hook called, fool!!!"))))

(register-hook *plugin* ItemUseHook
               (fn [hook]
                 (my-hook hook)))

(def kowboy-loc (loc "KowboyMac"))


(register-command *plugin*
                  {:aliases ["tntdrill"]
                   :permissions [""]
                   :description "turn a column into tnt"
                   :tooltip "/tntdrill"
                   :min 1 :max 1}
                  (fn [p [_]]
                    (when (instance? Player p)
                      (-> p
                          loc
                          block-seq
                          first
                          tnt-drill))))


(.notice (player "RileyRoo101") "Sorry")


(-> (player "KowboyMac")
    loc
    block-seq
    first)

(-> (player "KowboyMac")
    (.getTargetLookingAt))

(doto (-> (player "KowboyMac")
          (.getTargetLookingAt))
  (move 3 3 3))

(player-seq)


;; personal rain cloud
(defn rain-cloud-task [player]
  (let [fire-at (atom (System/currentTimeMillis))]
   (proxy [ServerTask] [(Canary/getServer) 0 true]
     (run []
       (when (< @fire-at (System/currentTimeMillis))
         (.makeLightningBolt (.getWorld player)
                             (loc player))
         (swap! fire-at + 3000 (rand 10000)))))))

(def kowboy-rain-cloud
  (rain-cloud-task (player "KowboyMac")))

(.addSynchronousTask (Canary/getServer) kowboy-rain-cloud)
(.removeSynchronousTask (Canary/getServer) kowboy-rain-cloud)

(move (player "RileyRoo101") 0 10 0)

(burn (player "KowboyMac") 600)
(set-health! (player "RileyRoo101") 0)

(max-health! (player "KowboyMac"))
