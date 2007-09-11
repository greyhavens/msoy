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
    public Component getComponent ();

    /**
     * Requests that this component move to the row and column supplied if possible.
     * @param highlight indicates whether the new location should be highlighted briefly
     */
    public void gotoLocation (int row, int column, boolean highlight);
}
