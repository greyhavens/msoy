//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a piece of furniture (any prop really) that a user can place into a virtual world
 * scene and potentially interact with.
 */
public class Furniture extends Item
{
    /** An action associated with this furniture which is dispatched to the virtual world client
     * when the furniture is clicked on (max length 255 characters). */
    public String action = "";

    /** The x position of the hot spot to use for this furniture. */
    public short hotSpotX;

    /** The y position of the hot spot to use for this furniture. */
    public short hotSpotY;

    @Override // from Item
    public byte getType ()
    {
        return FURNITURE;
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (_furniMedia != null);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
