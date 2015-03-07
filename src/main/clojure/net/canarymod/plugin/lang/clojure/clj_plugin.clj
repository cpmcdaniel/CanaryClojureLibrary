(ns net.canarymod.plugin.lang.clojure.clj-plugin
  (:gen-class :name net.canarymod.plugin.lang.clojure.CljPlugin
              :extends net.canarymod.plugin.Plugin
              :init init
              :state state
              :constructors {[net.canarymod.plugin.PluginDescriptor] []})
  (:use [clojure.tools.nrepl.server :only [start-server stop-server]]
        [cider.nrepl :only [cider-nrepl-handler]])
  (:import (net.canarymod.plugin Plugin PluginDescriptor Priority PluginListener)
           (net.canarymod.exceptions InvalidPluginException)
           (net.canarymod Canary Translator)
           (net.canarymod.logger Logman)
           (net.canarymod.hook Dispatcher Hook)
           (net.canarymod.commandsys CanaryCommand TabCompleteDispatch DynamicCommandAnnotation)
           (net.canarymod.config Configuration)))

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

(defn- start-repl
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

(defn- stop-repl [plugin]
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

(defn reload-plugin
  "Unregisters all hooks and commands for this plugin and re-registers them."
  ([]
   (reload-plugin *plugin*))
  ([plugin]
   (-ns-disable plugin)
   (-ns-enable plugin)))
