(ns com.cpmcdaniel.canary.clojure-plugin
  (:use net.canarymod.plugin.lang.clojure.clj-plugin))

;; This is a bare-bones plugin intended to be used for REPL experimentation
;; be sure to add `repl=true` to the ClojurePlugin.cfg file in the server
;; config directory (you will have to start the plugin at least once for it
;; to appear).

(defn enable
  "Enable the plugin"
  [plugin]
  (info "Enabling ClojurePlugin")
  true)

(defn disable
  "Disable the plugin"
  [plugin]
  (info "Disabling ClojurePlugin"))
