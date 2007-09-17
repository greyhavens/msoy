//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;

import com.threerings.msoy.swiftly.client.view.RemovalNotifier;

/**
 * Generically handles RemovalNotifiers actions with Maps.
 */
public class ComponentMapHandler <K, C> implements RemovalNotifier<C>
{
    /**
     * @param component The generic component being tracked.
     * @param jcomp The JComponent wrapped by the generic component.
     * @param components The map holding the components being tracked.
     */
    public ComponentMapHandler (final C component, JComponent jcomp, Map<K, C> components)
    {
        _components = components;

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
