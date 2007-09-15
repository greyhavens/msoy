//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Component;

import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Any {@link Component} wanting to be edited in the {@link TabbedEditorView} must implement this
 * interface.
 */
public interface TabbedEditorComponent extends PositionableComponent
{
    public PathElement getPathElement ();

    public void setPathElement (PathElement element);

    /**
     * Returns the Component actually editing the path element. In the case of a scrolled component
     * this would be the Component in the viewport
     */
    public Component getEditingComponent ();
}
