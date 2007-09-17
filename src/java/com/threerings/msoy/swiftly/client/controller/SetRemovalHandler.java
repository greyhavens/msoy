//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import java.util.Set;

import com.threerings.msoy.swiftly.client.view.RemovalNotifier;

/**
 * Generically handles RemovalNotifiers actions with Sets.
 */
public class SetRemovalHandler <C> implements RemovalNotifier<C>
{
    /**
     * Set<T> is the set holding the components to be removed.
     */
    public SetRemovalHandler (Set<C> components)
    {
        _components = components;
    }

    // from RemovalNotifier
    public void componentRemoved (C component)
    {
        _components.remove(component);
    }

    private final Set<C> _components;
}
