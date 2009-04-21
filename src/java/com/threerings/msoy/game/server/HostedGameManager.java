//
// $Id$

package com.threerings.msoy.game.server;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.EventThread;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.game.data.ParlorGameConfig;

import static com.threerings.msoy.Log.log;

/**
 * Manages games that have a sandboxed Java server component.
 */
@Singleton @EventThread
public class HostedGameManager
{
    /**
     * Returns the custom class loader that should be used for the specified game.
     */
    public ClassLoader getClassLoader (PlaceConfig config)
    {
        // if this is not a game config, we need not do anything fancy
        if (!(config instanceof ParlorGameConfig)) {
            return null;
        }

        ParlorGameConfig gconfig = (ParlorGameConfig)config;
        String manager = gconfig.getGameDefinition().manager;
        if (manager == null) {
            return null; // if no custom manager is specified, we're groovy
        }

        URLClassLoader loader = _loaders.get(manager);
        if (loader != null) {
            return loader;
        }

        log.info("Creating custom classloader for " + manager + ".");

        String path = gconfig.getGameDefinition().getMediaPath(gconfig.getGameId());
        try {
            loader = new URLClassLoader(new URL[] { new URL(path) }, getClass().getClassLoader());
            _loaders.put(manager, loader);
        } catch (Exception e) {
            log.warning("Failed to create URL for class loader [path=" + path + "].", e);
        }
        return loader;
    }

    /** Maps game manager class names to custom class loaders. */
    protected Map<String,URLClassLoader> _loaders = Maps.newHashMap();
}
