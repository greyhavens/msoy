//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.web.Decor;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains information on the location of furniture in a scene.
 */
public class DecorData extends FurniData
{
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
