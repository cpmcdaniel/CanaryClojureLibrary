(set-env!
 :source-paths   #{"src/main/java" "src/main/clojure"}
 :resource-paths #{"src/main/resources"}
 :dependencies   '[[cpmcdaniel/boot-with-pom "0.0.1" :scope "provided"]])

(task-options!
 aot  {:namespace     #{'net.canarymod.plugin.lang.clojure.clj-plugin}}
 uber {:exclude-scope #{"provided"}})

(require '[cpmcdaniel.boot-with-pom :refer :all])

(deftask build
   "Build my project"
   []
   (comp (with-pom) (aot) (javac) (uber) (jar) (install)))
