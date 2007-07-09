//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Represents an interactive piece of furniture. Something that lives permanently in a room but
 * which is interactive in some way.
 */
public class Toy extends Item
{
    // @Override from Item
    public byte getType ()
    {
        return TOY;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (furniMedia != null);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        if (furniMedia != null && furniMedia.isImage()) {
            return furniMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
