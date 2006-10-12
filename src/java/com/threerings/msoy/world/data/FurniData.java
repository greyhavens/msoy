//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains information on the location of furniture in a scene.
 */
public class FurniData extends SimpleStreamableObject
    implements Cloneable
{
    /** An actionType indicating that the furniture is 'background' and
     * not interactive. A background image is shown over the entire
     * scene. Background music auto-plays. */
    public static final byte BACKGROUND = -1;

    /** An actionType indicating 'no action'. */
    public static final byte ACTION_NONE = 0;

    /** An actionType indicating that actionData is a URL. */
    public static final byte ACTION_URL = 1;

    /** An actionType indicating that actionData is a game item id. */
    public static final byte ACTION_GAME = 2;

    /** An actionType indicating that we're a portal. actionData
     * will be in the format "targetScene:targetId", for now. */
    public static final byte ACTION_PORTAL = 3;

    /** The id of this piece of furni. */
    public int id;

    /** Identifies the type of the item that was used to create this furni,
     * or Item.NOT_A_TYPE. */
    public byte itemType;

    /** Identifies the id of the item that was used to create this. */
    public int itemId;

    /** Info about the media that represents this piece of furni. */
    public MediaDesc media;

    /** The location in the scene. */
    public MsoyLocation loc;

    /** A scale factor in the X direction. */
    public float scaleX = 1f;

    /** A scale factor in the Y direction. */
    public float scaleY = 1f;

    /** The type of action, determines how to use actionData. */
    public byte actionType;

    /** The action, interpreted using actionType. */
    public String actionData;

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
            (this.itemType == that.itemType) &&
            (this.itemId == that.itemId) &&
            this.media.equals(that.media) &&
            this.loc.equals(that.loc) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY) &&
            (this.actionType == that.actionType) &&
            ObjectUtil.equals(this.actionData, that.actionData);
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
