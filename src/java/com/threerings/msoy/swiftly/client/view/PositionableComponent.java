//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Component;

/**
 * Provides the implementing Component with methods to change the currently displayed position.
 */
public interface PositionableComponent
{
    /**
     * Returns the Component implementing this interface.
     */
    Component getComponent ();

    /**
     * Requests that this component move to the PositionLocation if possible.
     */
    void gotoLocation (PositionLocation location);
}
