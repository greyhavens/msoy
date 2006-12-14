//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents a pet, which is just furniture, really.
 */
public class Pet extends Item
{
    // @Override from Item
    public byte getType ()
    {
        return PET;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (furniMedia != null) && !nonBlank(name);
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
