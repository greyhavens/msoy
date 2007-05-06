//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Component;

/** 
 * Provides the implementing Component with methods to change the currently displayed position.  
 */
public interface PositionableComponent
{
    /**
     * Returns the Component implementing this interface.
     */
    public Component getComponent ();

    /**
     * Requests that this component move to the row and column supplied if possible.
     */
    public void gotoLocation (int row, int column);
}
