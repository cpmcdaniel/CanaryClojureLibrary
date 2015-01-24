package net.canarymod.plugin.lang.clojure;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import net.canarymod.Canary;
import net.canarymod.CanaryClassLoader;
import net.canarymod.exceptions.InvalidPluginException;
import net.canarymod.plugin.Plugin;
import net.canarymod.plugin.PluginDescriptor;

/**
 * Implementation for Clojure plugins. Responsible for initializing the Clojure
 * runtime and loading the main namespace (which must contain a public "enable"
 * function).
 *
 * @author Craig McDaniel
 */
public class ClojurePlugin extends Plugin {

    private String clojureNamespace;
    private PluginDescriptor desc;
    private CanaryClassLoader cl;

    public ClojurePlugin(PluginDescriptor desc, CanaryClassLoader cl) throws InvalidPluginException {
        this.desc = desc;
        this.cl = cl;

        clojureNamespace = desc.getCanaryInf().getString("clojure-namespace", "").trim();
        if ("".equals(clojureNamespace)) {
            throw new InvalidPluginException("clojure-namespace must be defined for Clojure plugins!");
        }
    }

    @Override
    public boolean enable() {
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            IFn req = Clojure.var("clojure.core", "require");
            req.invoke(Clojure.read(clojureNamespace));

            IFn enable = Clojure.var(clojureNamespace, "enable");
            Object ret = enable.invoke(this);

            if (ret instanceof Boolean) {
                return (Boolean) ret;
            }
            return ret != null;
        } catch (IllegalStateException e) {
            getLogman().error("Clojure plugin namespace must define an 'enable' function");
            return false;
        } finally {
            Thread.currentThread().setContextClassLoader(previousCL);
        }
    }

    @Override
    public void disable() {
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            IFn disable = Clojure.var(clojureNamespace, "disable");
            disable.invoke(this);
        } catch (IllegalStateException e) {
            getLogman().debug("No 'disable' function was found");
        } finally {
            Canary.hooks().unregisterPluginListeners(this);
            Canary.commands().unregisterCommands(this);
            Thread.currentThread().setContextClassLoader(previousCL);
        }
    }

    public void registerHook() {
        Canary.hooks();
    }

    public void registerCommand() {
        Canary.commands();
    }
}
