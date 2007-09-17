//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.threerings.msoy.swiftly.client.view.RemovalNotifier;

/**
 * Generically handles RemovalNotifiers actions with Maps.
 */
public class MapRemovalHandler <K, C> implements RemovalNotifier<C>
{
    /**
     * Map<K, T> is the map holding the components to be removed.
     */
    public MapRemovalHandler (Map<K, C> components)
    {
        _components = components;
    }

    // from RemovalNotifier
    public void componentRemoved (C component)
    {
        Iterator<Entry<K, C>> iter = _components.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, C> entry = iter.next();
            if (entry.getValue().equals(component)) {
                iter.remove();
            }
        }
    }

    private final Map<K, C> _components;
}
