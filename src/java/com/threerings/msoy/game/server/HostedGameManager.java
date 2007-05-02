//
// $Id$

package com.threerings.msoy.game.server;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.web.client.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Manages games that have a sandboxed Java server component.
 */
public class HostedGameManager
{
    /**
     * Returns the custom class loader that should be used for the specified game.
     */
    public ClassLoader getClassLoader (PlaceConfig config)
    {
        // if this is not a game config, we need not do anything fancy
        if (!(config instanceof MsoyGameConfig)) {
            return null;
        }

        MsoyGameConfig gconfig = (MsoyGameConfig)config;
        String manager = gconfig.getGameDefinition().manager;
        if (manager == null) {
            return null; // if no custom manager is specified, we're groovy
        }

        URLClassLoader loader = _loaders.get(manager);
        if (loader != null) {
            return loader;
        }

        log.info("Creating custom classloader for " + manager + ".");

        ArrayList<URL> ulist = new ArrayList<URL>();
        String path = "";
        try {
            path = gconfig.getGameDefinition().getMediaPath(gconfig.getGameId());
            ulist.add(new URL(path));
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create URL for class loader [path=" + path + "].", e);
        }

        loader = new URLClassLoader(
            ulist.toArray(new URL[ulist.size()]), getClass().getClassLoader());
        _loaders.put(manager, loader);
        return loader;
    }

    /** Maps game manager class names to custom class loaders. */
    protected HashMap<String,URLClassLoader> _loaders = new HashMap<String,URLClassLoader>();
}
