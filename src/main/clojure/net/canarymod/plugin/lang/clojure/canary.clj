(ns net.canarymod.plugin.lang.clojure.canary
  (:import (net.canarymod.plugin Priority PluginListener)
           (net.canarymod Canary Translator BlockIterator)
           (net.canarymod.logger Logman)
           (net.canarymod.api.entity.living LivingBase)
           (net.canarymod.api.entity.living.humanoid Player)
           (net.canarymod.api.entity EntityType Entity)
           (net.canarymod.api.world World DimensionType)
           (net.canarymod.api.world.position Position Location)
           (net.canarymod.api.world.blocks Block BlockType BlockFace)
           (net.canarymod.hook Dispatcher Hook)
           (net.canarymod.commandsys CanaryCommand TabCompleteDispatch DynamicCommandAnnotation)
           (net.canarymod.api.inventory Item)))


(def ^{:dynamic 1} *plugin* nil)
(def ^{:dynamic 1} *logman* (Logman/getLogman "ClojurePlugin"))

(defmacro with-plugin
  "Creates thread-local bindings for *plugin* and *logman*."
  [plugin & body]
  `(binding [*plugin* ~plugin
             *logman* (.getLogman ~plugin)]
     ~@body))


(defn register-hook
  "Register a hook with CanaryMod.
  Takes the plugin instance, the hook-type (Java class), and hook-fn. The
  hook-fn will be passed a single argument, an instance of hook-type.
  Optionally takes a Priority as the final argument."
  ([plugin hook-type hook-fn]
    (register-hook plugin hook-type hook-fn Priority/NORMAL))
  ([plugin hook-type hook-fn priority]
    (.registerHook (Canary/hooks)
                   (reify PluginListener)
                   plugin
                   hook-type
                   (proxy [Dispatcher] []
                     (execute [^PluginListener listener ^Hook hook]
                       (with-plugin plugin
                                    (hook-fn hook))))
                   priority)))

(defn- string-array [coll]
  (into-array java.lang.String coll))

(defn- as-command-annotation
  "Converts a map of command configuration to Command annotation data using
  DynamicCommandAnnotation class."
  [{:keys [aliases permissions description tooltip parent help-lookup
           search-terms min max version]
    :or {aliases [] permissions [] description "" tooltip "" parent ""
         help-lookup "" search-terms [] min 1 max -1 version 1}}]
  (DynamicCommandAnnotation. (string-array aliases)
                             (string-array permissions)
                             description tooltip parent help-lookup
                             (string-array search-terms)
                             min max "" version))

(defn register-command
  "Registers a command function.
  Takes the plugin instance, a map of command configuration data, and the
  function to execute. Optionally takes a boolean force argument (defaults
  to false)."
  ([plugin command-meta command]
    (register-command plugin command-meta command false))

  ([plugin command-meta command force]
    (let [tab-complete (or (:tab-complete command-meta)
                           (constantly []))                    ; no completion, by default
          tcd (reify TabCompleteDispatch
                (complete [this caller args]
                  (tab-complete caller args)))]
      (.registerCommand (Canary/commands)
                        (proxy [CanaryCommand]
                               [(as-command-annotation command-meta)
                                plugin
                                (or (:translator command-meta) (Translator/getInstance))
                                tcd]
                          (execute [caller args]
                            (with-plugin plugin
                                         (command caller args))))
                        plugin
                        force))))

;; Logging functions
(defn info [msg]
  (.info *logman* msg))

(defn error [msg]
  (.error *logman* msg))

(defn debug [msg]
  (.debug *logman* msg))

(defn warn [msg]
  (.warn *logman* msg))

(defn trace [msg]
  (.trace *logman* msg))

(defn reload-plugin
  "Unregisters all hooks and commands for this plugin and re-registers them."
  []
  (.reload *plugin*))


;; Canary API Wrappers ;;

;; Player functions ;;
(defn player
  "Gets the player by name"
  [player-name]
  (.. Canary getServer (getPlayer player-name)))

(defprotocol Locator
  (loc [this]))

(extend-protocol Locator
  String
  (loc [player-name]
    (when-let [p (player player-name)]
      (.getLocation p)))
  Location
  (loc [this] this)
  Object
  (loc [this]
    (.getLocation this)))

(defn player-seq
  "Gets a sequence of Players currently connected."
  []
  (seq (.. Canary getServer getPlayerList)))



;; Entity functions ;;

(defn spawn-entity
  "spawns an entity at the specified location"
  [^Location loc ^EntityType entity-type]
  (doto
      (.. (Canary/factory) (getEntityFactory) (newEntity entity-type loc))
    (.spawn)))

(defprotocol Moveable
  (move [this x y z])
  (move-x [this x])
  (move-y [this y])
  (move-z [this z]))

(extend-protocol Moveable
  Location
  (move [this x y z] (doto this (.move x y z)))
  (move-x [this x] (doto this (.moveX x)))
  (move-y [this y] (doto this (.moveY y)))
  (move-z [this z] (doto (.moveZ z)))

  Entity
  (move [this x y z] (doto this (.moveEntity x y z)))
  (move-x [this x] (doto this (.moveEntity x 0 0)))
  (move-y [this y] (doto this (.moveEntity 0 y 0)))
  (move-z [this z] (doto this (.moveEntity 0 0 z))))

(defn pitch
  "Gets the pitch of an Entity or Location"
  [thing]
  (.getPitch thing))

(defn rotation
  "Gets the rotation of an Entity or Location"
  [thing]
  (.getRotation thing))

(defn make-explosion
  "Create an explosion centered at the entity, with the specified explosive power."
  [^Entity entity power]
  (let [^Location loc (.getLocation entity)]
    (.. entity (getWorld) (makeExplosion entity
                                         (.getX loc) (.getY loc) (.getZ loc)
                                         power true))))

(defn kill!
  "Kill a living entity"
  [^LivingBase entity]
  (.setHealth entity 0)
  (.kill entity))

(defn set-health!
  "Sets the health of a living entity"
  [^LivingBase entity hp]
  (.setHealth entity hp))

(defn max-health!
  "Sets the living entity's health to max"
  [^LivingBase entity]
  (set-health! entity (.getMaxHealth entity)))

(defprotocol Explodable
  (explode [this power]))

(extend-protocol Explodable
  Entity
  (explode [this power]
    (make-explosion this power))
  LivingBase
  (explode [this power]
    (kill! this)
    (make-explosion this power)))

(defn burn
  "Set an entity on fire"
  [^Entity entity fire-ticks]
  (.setFireTicks entity fire-ticks))



;; Block functions ;;

(defn set-block-type!
  "Changes the block type"
  [^Block block ^BlockType block-type]
  (.setBlockAt (.getWorld block) (.getLocation block) block-type))

(defn block-seq
  "Creates a block seq along a Location's line of sight"
  ([^Location loc]
   (iterator-seq (BlockIterator. loc)))
  ([^Location loc do-air?]
   (iterator-seq (BlockIterator. loc do-air?)))
  ([^Location loc in-range in-step]
   (iterator-seq (BlockIterator. loc in-range in-step)))
  ([^Location loc in-range in-step do-air?]
   (iterator-seq (BlockIterator. loc in-range in-step do-air?))))

(defn vertical-shaft
  "Given a block, returns a seq of the column of blocks beneath it (inclusive)."
  [^Block block]
  (cons block
        (lazy-seq (when (< 0 (.getY block))
                    (vertical-shaft (.getRelative block 0 -1 0))))))

(defn tnt-drill
  "Drills a vertical shaft of TNT ready to be lit."
  [^Block top-block]
  (doseq [block (vertical-shaft top-block)]
    (set-block-type! block BlockType/TNT)))
