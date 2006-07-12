//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.MediaData;

/**
 * Contains information on the location of furniture in a scene.
 */
public class FurniData extends SimpleStreamableObject
    implements Cloneable
{
    /** The id of this piece of furni. */
    public int id;

    /** Info about the media that represents this piece of furni. */
    public MediaData media;

    /** The location in the scene. */
    public MsoyLocation loc;

    /** A scale factor in the X direction. */
    public float scaleX = 1f;

    /** A scale factor in the Y direction. */
    public float scaleY = 1f;

    /** The action associated with this furniture. */
    public Object action;

    // documentation inherited
    public boolean equals (Object other)
    {
        return (other instanceof FurniData) &&
            ((FurniData) other).id == this.id;
    }

    // documentation inherited
    public int hashCode ()
    {
        return id;
    }

    /**
     * @return true if the other FurniData is identical.
     */
    public boolean equivalent (FurniData that)
    {
        return (this.id == that.id) &&
            this.media.equals(that.media) &&
            this.loc.equals(that.loc) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY) &&
            ObjectUtil.equals(this.action, that.action);
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
