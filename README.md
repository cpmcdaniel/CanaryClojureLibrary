# CanaryClojureLibrary
Language extension for writing CanaryMod plugins in Clojure

# Installation

Source-only insall is supported at this time. Requires CanaryMod >= 1.2.0.

- Clone the repository
- Run `mvn install` from the project root
- Copy *target/clojurepluginlib-VERSION-independent.jar* into the CanaryMod server's *pluginlangs* directory (note: this was added in CanaryMod 1.2.0)

# Writing a Clojure plugin

Include the following dependency in your Leiningen project.clj or Maven POM.

[](dependency)
```clojure
[net.canarymod/clojurepluginlib "1.0.0-SNAPSHOT"] ;; latest release
```
[](/dependency)

You will also need to add the repository for CanaryMod itself:

```clojure
:repositories [["vi-repo" "http://repo.visualillusionsent.net/repository/public/"]]
```

You need a namespace that will serve as an entry point for loading the plugin. The following represents a bare-bones plugin implementation, followed by the *Canary.inf* descriptor file for the plugin.

```clojure
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
```

```inf
language=clojure
name=ClojurePlugin
version=0.1.0-SNAPSHOT
author=Craig McDaniel
clojure-namespace=com.cpmcdaniel.canary.clojure-plugin
```

To enable the Clojure REPL for the plugin above, open the *config/ClojurePlugin/ClojurePlugin.cfg* file and add `repl=true`

Finally, package the Clojure code and Canary.inf into a JAR file and copy it to the CanaryMod server *plugins* directory.

Examples of registering hooks and commands can be found in the *examples/* directory.

# TODOs:
- Add API calls for Plugin logging
- Add documentation for register-hook, register-command, and reload-plugin
