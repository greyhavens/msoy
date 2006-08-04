//
// $Id$

package com.threerings.msoy.item.data;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
public class Furniture extends MediaItem
{
    /** An action associated with this furniture which is dispatched to the
     * virtual world client when the furniture is clicked on (max length 255
     * characters). */
    public String action;

    /** A description of this piece of furniture (max length 255 characters). */
    public String description;

    // @Override from Item
    public String getType ()
    {
        return "FURNITURE";
    }

    // @Override from Item
    public String getInventoryDescrip ()
    {
        return toInventoryDescrip(description);
    }
}
