//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains information on the location of furniture in a scene.
 */
public class FurniData extends SimpleStreamableObject
    implements Cloneable, DSet.Entry
{
    /** An actionType indicating 'no action'.
     *  actionData = null to capture mouse events, or "-" to pass through. */
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

    /** An actionType indicating that actionData is special page displayed in the chat panel.
     *  actionData = "<tabName>:<pageURL>" */
    public static final byte ACTION_HELP_PAGE = 5;

    /** The id of this piece of furni. */
    public short id;

    /** Identifies the type of the item that was used to create this furni, or Item.NOT_A_TYPE. */
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

    /** Rotation angle in degrees. */
    public float rotation = 0f;

    /** The x location of this furniture's hot spot. */
    public short hotSpotX;

    /** The y location of this furniture's hot spot. */
    public short hotSpotY;

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
     * Return the actionData as two strings, split after the first colon.  If there is no colon,
     * then a single-element array is returned.
     */
    public String[] splitActionData ()
    {
        if (actionData == null) {
            return new String[] { null };
        }
        String sep = (actionType == ACTION_URL) ? "||" : ":";
        int sepDex = actionData.indexOf(sep);
        if (sepDex == -1) {
            return new String[] { actionData };

        } else {
            return new String[] { actionData.substring(0, sepDex),
                actionData.substring(sepDex + sep.length()) };
        }
    }

    // from DSet.Entry
    public Comparable getKey ()
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
            (this.rotation == that.rotation) &&
            (this.hotSpotX == that.hotSpotX) &&
            (this.hotSpotY == that.hotSpotY) &&
            (this.actionType == that.actionType) &&
            ObjectUtil.equals(this.actionData, that.actionData);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FurniData) && ((FurniData) other).id == this.id;
    }

    @Override // from Object
    public int hashCode ()
    {
        return id;
    }

    @Override // from Object
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

    @Override // from Object
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // not going to happen
        }
    }
}
