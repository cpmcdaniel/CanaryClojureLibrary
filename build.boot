(set-env!
 :source-paths   #{"src/main/java" "src/main/clojure"}
 :resource-paths #{"src/main/resources"}
 :dependencies   '[[cpmcdaniel/boot-with-pom "1.0" :scope "provided"]
                   [cpmcdaniel/boot-copy "1.0" :scope "provided"]])

(require '[cpmcdaniel.boot-with-pom :refer :all]
         '[cpmcdaniel.boot-copy :refer :all]
         '[clojure.pprint :refer [pprint]]
         '[clojure.java.io :as io]
         '[boot.util :as util])

(task-options!
 aot  {:namespace     #{'net.canarymod.plugin.lang.clojure.clj-plugin
                        'net.canarymod.plugin.lang.clojure.canary}}
 uber {:exclude-scope #{"provided"}}
 copy {:output-dir    "/Users/cmcdaniel/Desktop/server/pluginlangs"
       :matching       #{#"\.jar$"}})



(deftask build
   "Build my project"
   []
   (comp (watch) (speak) (with-pom) (aot) (javac) (uber) (jar) (install) (copy)))
