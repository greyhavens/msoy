//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a pet, which is just furniture, really.
 */
public class Pet extends Item
{
    /** Orders this pet to stay in the current room, binding them to this room so that they will be
     * automatically loaded when anyone comes into the room. */
    public static final int ORDER_STAY = 1;

    /** Orders this pet to follow the caller (who must be its owner). Any existing "stay" binding
     * will not be modified. */
    public static final int ORDER_FOLLOW = 2;

    /** Orders this pet to stop following the caller (who must be its owner). If the pet is bound
     * to a room, and that room is resolved, it will return to that room, otherwise it will be
     * unloaded. */
    public static final int ORDER_GO_HOME = 3;

    /** Orders this pet to unbind from any room and unload itself. The pet's owner may issue this
     * order in any circumstance, alternatively a player with modification privileges on a room may
     * issue this order on any pet that is bound to that room. */
    public static final int ORDER_SLEEP = 4;

    /**
     * Returns a <code>MediaDesc</code> configured to display a pet as a static image.
     */
    public static MediaDesc getStaticImagePetMedia ()
    {
        return new DefaultItemMediaDesc(MediaDesc.IMAGE_PNG, PET, "static");
    }
    
    @Override // from Item
    public byte getType ()
    {
        return PET;
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (_furniMedia != null) && nonBlank(name, MAX_NAME_LENGTH) &&
            (_furniMedia.isSWF() || _furniMedia.isRemixable());
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
