(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources" "src"}
 :dependencies   '[[cpmcdaniel/boot-copy "1.0" :scope "provided"]
                   [net.canarymod/clojurepluginlib "1.0.0-SNAPSHOT" :scope "provided"]]
 :repositories   #(conj % ["vi-repo" "http://repo.visualillusionsent.net/repository/public/"]))


(require '[cpmcdaniel.boot-copy :refer :all])

(task-options!
 pom  {:project 'cpmcdaniel/clojure-plugin
       :version "1.0-SNAPSHOT"
       :url "https://github.com/cpmcdaniel/CanaryClojureLibrary/examples/clojure-plugin"
       :license {"BSD 3-Clause License" "http://opensource.org/licenses/BSD-3-Clause"}}
 copy {:output-dir "/Users/cmcdaniel/Desktop/server/plugins"
       :matching   #{#"\.jar$"}})

(deftask build []
  (comp (watch) (pom) (jar) (copy)))
