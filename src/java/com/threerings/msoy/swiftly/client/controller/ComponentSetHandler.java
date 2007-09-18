//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.JComponent;

/**
 * Generically handles RemovalNotifiers actions with Sets.
 */
public class ComponentSetHandler<C>
{
    /**
     * @param component The generic component being tracked.
     * @param jcomp The JComponent wrapped by the generic component.
     * @param components The set holding the components being tracked.
     */
    public ComponentSetHandler (final C component, JComponent jcomp, Set<C> components)
    {
        _components = components;
        _components.add(component);

        // this appears to be the only reliable way to hook into JComponent.removeNotify()
        jcomp.addPropertyChangeListener("ancestor", new PropertyChangeListener () {
            public void propertyChange (PropertyChangeEvent evt)
            {
                // if the parent is now null, remove the component.
                if (evt.getNewValue() == null) {
                    componentRemoved(component);
                }
            }
        });
    }

    private void componentRemoved (C component)
    {
        _components.remove(component);
    }

    private final Set<C> _components;
}