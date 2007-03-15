//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains information on the location of furniture in a scene.
 */
public class DecorData extends SimpleStreamableObject
    implements Cloneable
{
    /** The id of this piece of decor. */
    public short id;

    /** Info about the media that represents this piece of decor. */
    public MediaDesc media;

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
    public boolean equals (Object other)
    {
        return (other instanceof DecorData) &&
            ((DecorData) other).id == this.id;
    }

    // documentation inherited
    public int hashCode ()
    {
        return id;
    }

    /**
     * @return true if the other DecorData is identical.
     */
    public boolean equivalent (DecorData that)
    {
        return this.id == that.id &&
            this.media.equals(that.media) &&
            this.type == that.type &&
            this.height == that.height &&
            this.width == that.width &&
            this.depth == that.depth &&
            this.horizon == that.horizon;
    }

    @Override
    public String toString ()
    {
        String s = "Decor[id=" + id + ", type=" + type + "]";
        return s;
    }

    // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // not going to happen
        }
    }
}
