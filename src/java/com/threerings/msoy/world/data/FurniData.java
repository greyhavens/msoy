//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on the location of furniture in a scene.
 */
public class FurniData extends SimpleStreamableObject
    implements Cloneable
{
    /** An actionType indicating 'no action'.
     * actionData = null to capture mouse events, or "-" to pass through. */
    public static final byte ACTION_NONE = 0;

    /** An actionType indicating that actionData is a URL.
     *  actionData = "<url>" */
    public static final byte ACTION_URL = 1;

    /** An actionType indicating that actionData is a lobby game item id.
     *  actionData = "<gameId>:<gameName>" */
    public static final byte ACTION_LOBBY_GAME = 2;

    /** An actionType indicating that we're a portal.
     *  actionData = "<targetSceneId>:<targetSceneName>" */
    public static final byte ACTION_PORTAL = 3;

    /** An actionType indicating that actionData is a world game item id.
     *  actionData = "<gameId>:<gameName>" */
    public static final byte ACTION_WORLD_GAME = 4;
    
    /** The id of this piece of furni. */
    public short id;

    /** Identifies the type of the item that was used to create this furni,
     * or Item.NOT_A_TYPE. */
    public byte itemType;

    /** Identifies the id of the item that was used to create this. */
    public int itemId;

    /** Info about the media that represents this piece of furni. */
    public MediaDesc media;

    /** The location in the scene. */
    public MsoyLocation loc;

    /** Layout information, used for perspectivization, etc. */
    public byte layoutInfo;

    /** A scale factor in the X direction. */
    public float scaleX = 1f;

    /** A scale factor in the Y direction. */
    public float scaleY = 1f;

    /** The type of action, determines how to use actionData. */
    public byte actionType;

    /** The action, interpreted using actionType. */
    public String actionData;

    /**
     * Returns the identifier for the item for which we're presenting a visualization.
     */
    public ItemIdent getItemIdent ()
    {
        return new ItemIdent(itemType, itemId);
    }

    /**
     * Return the actionData as two strings, split after the first colon.
     * If there is no colon, then a single-element array is returned.
     */
    public String[] splitActionData ()
    {
        if (actionData == null) {
            return new String[] { null };
        }
        int colonDex = actionData.indexOf(':');
        if (colonDex == -1) {
            return new String[] { actionData };

        } else {
            return new String[] { actionData.substring(0, colonDex),
                actionData.substring(colonDex + 1) };
        }
    }

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
            (this.layoutInfo == that.layoutInfo) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY) &&
            (this.actionType == that.actionType) &&
            ObjectUtil.equals(this.actionData, that.actionData);
    }

    @Override
    public String toString ()
    {
        String s = "Furni[id=" + id + ", itemType=" + itemType;
        if (itemType != Item.NOT_A_TYPE) {
            s += ", itemId=" + itemId;
        }
        s += ", actionType=" + actionType;
        if (actionType != ACTION_NONE) {
            s += ", actionData=\"" + actionData + "\"";
        }
        s += "]";

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
