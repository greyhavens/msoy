//
// $Id$

package com.threerings.msoy.item.web;

/**
 * A digital item representing a simple text document.
 */
public class Document extends MediaItem
{
    /** The title of this document (max length 255 characters). */
    public String title;

    // @Override from Item
    public String getType ()
    {
        return "DOCUMENT";
    }

    // @Override from Item
    public String getInventoryDescrip ()
    {
        return toInventoryDescrip(title);
    }
}
