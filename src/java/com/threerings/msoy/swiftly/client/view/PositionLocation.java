//
// $Id$

package com.threerings.msoy.swiftly.client.view;

/**
 * Used by PositionableComponent to store position information.
 */
public class PositionLocation
{
    public final int row;
    public final int column;

    /** Indicates whether the new location should be highlighted briefly. */
    public final boolean highlight;

    public PositionLocation (int row, int column, boolean highlight)
    {
        this.row = row;
        this.column = column;
        this.highlight = highlight;
    }
}
