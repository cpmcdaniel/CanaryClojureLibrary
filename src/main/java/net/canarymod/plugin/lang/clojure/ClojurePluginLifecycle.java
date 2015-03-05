package net.canarymod.plugin.lang.clojure;

import net.canarymod.CanaryClassLoader;
import net.canarymod.exceptions.PluginLoadFailedException;
import net.canarymod.logger.Logman;
import net.canarymod.plugin.Plugin;
import net.canarymod.plugin.PluginDescriptor;
import net.canarymod.plugin.lifecycle.PluginLifecycleBase;

import java.io.File;

/**
 * Lifecycle manager for Clojure plugin.
 *
 * @author Craig McDaniel
 */
public class ClojurePluginLifecycle extends PluginLifecycleBase {
    private CanaryClassLoader cl;

    public ClojurePluginLifecycle(PluginDescriptor desc) {
        super(desc);
    }

    @Override
    protected void _load() throws PluginLoadFailedException {
        try {
            cl = new CanaryClassLoader(new File(desc.getPath()).toURI().toURL(),
                                       getClass().getClassLoader());
            Thread.currentThread().setContextClassLoader(cl);

            //A hacky way of getting the name in during the constructor/initializer
            Plugin.threadLocalName.set(desc.getName());

            Plugin p = new CljPlugin(desc);

            //If it isn't called in initializer, gotta set it here.
            p.setName(desc.getName());
            p.setPriority(desc.getPriority());
            desc.setPlugin(p);
        } catch (Exception e) {
            throw new PluginLoadFailedException("Failed to load plugin", e);
        }
    }

    @Override
    protected void _unload() {
        if (cl != null) {
            cl.close();
        }
    }
}
