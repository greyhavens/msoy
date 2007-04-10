//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.web.Decor;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Contains information on the location of furniture in a scene.
 */
public class DecorData extends FurniData
{
    /** Default decor background. */
    public static final MediaDesc defaultMedia =
        new StaticMediaDesc(MediaDesc.IMAGE_PNG, Item.DECOR, Item.FURNI_MEDIA);

    /** Default decor location. */
    public static final MsoyLocation defaultLocation = new MsoyLocation (0.5, 0, 0, 0);
    
    /** Room type. Controls how the background wallpaper image is handled. */
    public byte type;
    
    /** Room height, in pixels. */
    public short height;
    
    /** Room width, in pixels. */
    public short width;

    /** Room depth, in pixels. */
    public short depth;

    /** Horizon position, in [0, 1]. */
    public float horizon;

    /**
     * Constructor, creates a data object with default values.
     */
    public DecorData ()
    {
        itemId = 0; // doesn't correspond to an object
        id = 0;     // it's not an actual furni
        media = defaultMedia;
        depth = 400;
        width = 800;
        height = 494;
        horizon = .5f;
        loc = defaultLocation;
    }
    
    /**
     * Helper function: specifies that this decor data structure has already been
     * populated from a Decor item object.
     */
    public boolean isInitialized ()
    {
        return itemId != 0;
    }
    
    // documentation inherited
    @Override
    public boolean equals (Object other)
    {
        return (other instanceof DecorData) &&
            ((DecorData) other).id == this.id;
    }

    // documentation inherited
    @Override
    public int hashCode ()
    {
        return id;
    }


    @Override
    public boolean equivalent (FurniData that)
    {
        if (! (that instanceof DecorData)) {
            return false;
        } else {
            DecorData data = (DecorData) that;
            return super.equivalent(that) &&
                this.type == data.type &&
                this.height == data.height &&
                this.width == data.width &&
                this.depth == data.depth &&
                this.horizon == data.horizon;
        }
    }

    @Override
    public String toString ()
    {
        String s = "Decor[itemId=" + itemId + ", type=" + type + ", media=" + media + "]";
        return s;
    }
}
