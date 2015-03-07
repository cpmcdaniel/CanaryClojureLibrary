(ns com.cpmcdaniel.canary.clojure-plugin
  (:use net.canarymod.plugin.lang.clojure.clj-plugin))

(defn enable
  "Enable the plugin"
  [plugin]
  (info "Enabling ClojurePlugin")
  true)

(defn disable
  "Disable the plugin"
  [plugin]
  (info "Disabling ClojurePlugin"))
