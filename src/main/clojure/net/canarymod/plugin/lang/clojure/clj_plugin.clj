(ns net.canarymod.plugin.lang.clojure.clj-plugin
  (:gen-class :name net.canarymod.plugin.lang.clojure.CljPlugin
              :extends net.canarymod.plugin.Plugin
              :init init
              :state state
              :constructors {[net.canarymod.plugin.PluginDescriptor] []}
              :methods [[reload [] java.lang.Object]])
  (:require [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [net.canarymod.plugin.lang.clojure.canary :refer :all])
  (:import (net.canarymod.plugin Plugin PluginDescriptor Priority PluginListener)
           (net.canarymod.exceptions InvalidPluginException)
           (net.canarymod Canary Translator)
           (net.canarymod.logger Logman)
           (net.canarymod.hook Dispatcher Hook)
           (net.canarymod.commandsys CanaryCommand TabCompleteDispatch DynamicCommandAnnotation)
           (net.canarymod.config Configuration)))

;;; REPL setup ;;;
(def ^{:private 1} repls (atom {}))

(defn- start-repl?
  "Reads the 'repl' property from the plugin configuration, which defaults to 'off'.
   Can be changed to 'on' for development purposese, but should be left off in a
   multiplayer server."
  [plugin]
  (.getBoolean (Configuration/getPluginConfig plugin) "repl" false))

(declare stop-repl)

(defn- get-port [repls]
  (inc (reduce max 9960 (map :port (vals repls)))))

(defn start-repl
  "Will start a REPL if configured to do so and if there is not already a REPL
  running for this plugin."
  [plugin]
  (when (and
         (start-repl? plugin)                              ; should we start a repl?
         (not (contains? @repls plugin)))                  ; already started?
    (let [port (get-port @repls)]
      (info (format "Starting REPL on port %d" port))
      (swap! repls
             #(assoc % plugin
                     (with-plugin plugin                   ; this may be in another thread
                       (start-server :port (get-port @repls)
                                     :handler cider-nrepl-handler))))
      (debug (format "%d REPLs are running" (count @repls))))))

(defn stop-repl [plugin]
  (when-let [repl (get @repls plugin)]
    (debug (format "%d REPLs are running" (count @repls)))
    (info (format "Stopping REPL on port %d" (:port repl)))
    (stop-server repl)
    (swap! repls #(dissoc % plugin))))

(defn -init [^PluginDescriptor desc]
  (let [plugin-ns (.trim (.getString (.getCanaryInf desc) "clojure-namespace" ""))]
    (if (empty? plugin-ns)
      (throw (InvalidPluginException. "clojure-namespace must be defined for Clojure plugins!"))
      [[] (atom {:plugin-descriptor desc
                 :plugin-ns plugin-ns
                 :name (.getName desc)
                 :priority (.getPriority desc)})])))

(defn -plugin-ns [this]
  (:plugin-ns @(.state this)))

(defn -enable-fn [this]
  (if-let [enable-fn (:enable @(.state this))]
    enable-fn
    (let [plugin-ns-sym (symbol (-plugin-ns this))
          enable-fn (do (require plugin-ns-sym)
                        (ns-resolve plugin-ns-sym (symbol "enable")))]
      (swap! (.state this) (fn [state] (assoc state :enable enable-fn)))
      enable-fn)))

(defn -ns-enable [this]
  (let [enable (-enable-fn this)]
    (info (str "Enabling ns: " (-plugin-ns this)))
    (enable this)))

(defn -enable [this]
  (with-plugin this
               (start-repl this)
               (-ns-enable this)))

(defn -ns-disable [this]
  (info (str "Disabling ns: " (-plugin-ns this)))
  (.unregisterPluginListeners (Canary/hooks) this)
  (.unregisterCommands (Canary/commands) this))

(defn -disable [this]
  (with-plugin this
               (stop-repl this)
               (-ns-disable this)))

(defn -reload [this]
  (-ns-disable this)
  (-ns-enable this))
