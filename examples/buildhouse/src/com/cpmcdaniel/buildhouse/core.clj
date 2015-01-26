(ns com.cpmcdaniel.buildhouse.core
  (:use [net.canarymod.plugin.lang.clojure.clj-plugin
         :only [info register-command]]))

(defn enable
  "Enable the Build House plugin"
  [plugin]
  (info "Enabling the Build House plugin")
  (register-command plugin {:aliases ["buildhouse"]
                            :permissions [""]
                            :description "builds a safe house intended for new players/worlds"
                            :tooltip "/buildhouse"
                            :min 1
                            :max 1}
                    (fn [_ _]
                      (info "Building a house!")))
  true)
