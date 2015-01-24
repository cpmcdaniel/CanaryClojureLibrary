(ns net.canarymod.plugin.lang.clojure.core
  (:use [clojure.tools.nrepl.server :only [start-server stop-server]])
  (:import (net.canarymod.plugin PluginListener Priority)
           (net.canarymod.hook Hook Dispatcher)
           (net.canarymod Canary Translator)
           (net.canarymod.commandsys DynamicCommandAnnotation CanaryCommand TabCompleteDispatch)))

(def ^{:private true} repls (atom {}))

(declare stop-repl)

(defn start-repl [plugin port]
  (if (contains? @repls plugin)
    (stop-repl plugin)
    (swap! repls #(assoc % plugin (start-server :port port)))))

(defn stop-repl [plugin]
  (stop-server (get @repls plugin))
  (swap! repls #(dissoc % plugin)))

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
                       (hook-fn hook)))
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
                            (command caller args)))
                        plugin
                        force))))