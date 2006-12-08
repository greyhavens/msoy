//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents a pet, which is just furniture, really.
 */
public class Pet extends Item
{
    /** A description of this pet (max length 255 characters). */
    public String description;

    // @Override from Item
    public byte getType ()
    {
        return PET;
    }

    // @Override from Item
    public String getDescription ()
    {
        return description;
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (furniMedia != null) &&
            nonBlank(description);
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
