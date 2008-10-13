//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

public class FurniData
    implements Cloneable, Hashable, Streamable, DSet_Entry
{
    /** An actionType indicating 'no action'.
        actionData = null to capture mouse events, or "-" to pass through. */
    public static const ACTION_NONE :int = 0;

    /** An actionType indicating that actionData is a URL.
        actionData = "<url>" */
    public static const ACTION_URL :int = 1;

    /** An actionType indicating that actionData is a lobby game item id.
        actionData = "<gameId>:<gameName>" */
    public static const ACTION_LOBBY_GAME :int = 2;

    /** An actionType indicating that we're a portal.
        actionData = "<targetSceneId>:<targetLocX>:<targetLocY>:<targetLocZ>:
        <targetLocOrient>:<targetSceneName>" */
    public static const ACTION_PORTAL :int = 3;

    /** An actionType indicating that actionData is a world game item id.
        actionData = "<gameId>:<gameName>" */
    public static const ACTION_WORLD_GAME :int = 4;

    /** An actionType indicating that actionData is a special help page to be displayed in a 
     * special way actionData = "<tabName>:<pageUrl>" TODO: this will change. */
    public static const ACTION_HELP_PAGE :int = 5;

    /** The id of this piece of furni. */
    public var id :int;

    /** Identifies the type of the item that was used to create this furni, or Item.NOT_A_TYPE. */
    public var itemType :int;

    /** Identifies the id of the item that was used to create this. */
    public var itemId :int;

    /** Info about the media that represents this piece of furni. */
    public var media :MediaDesc;

    /** Layout information, used for perspectivization, etc. */
    public var layoutInfo :int;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    /** A scale factor in the X direction. */
    public var scaleX :Number = 1;

    /** A scale factor in the Y direction. */
    public var scaleY :Number = 1;

    /** Rotation angle in degrees. */
    public var rotation :Number = 0;

    /** The x location of this furniture's hot spot. */
    public var hotSpotX :int;

    /** The y location of this furniture's hot spot. */
    public var hotSpotY :int;

    /** The type of action, determines how to use actionData. */
    public var actionType :int;

    /** The action, interpreted using actionType. */
    public var actionData :String;

    /**
     * Returns the identifier for the item for which we're presenting a visualization.
     */
    public function getItemIdent () :ItemIdent
    {
        return new ItemIdent(itemType, itemId);
    }

    /**
     * Return the actionData as strings separated by colons. If there is not at least one colon,
     * then a single-element array is returned.
     */
    public function splitActionData () :Array
    {
        if (actionData == null) {
            return [ null ];
        }
        var sep :String = (actionType == ACTION_URL) ? "||" : ":";
        var sepDex :int = actionData.indexOf(sep);
        if (sepDex == -1) {
            return [ actionData ];
        }
        if (actionType == ACTION_PORTAL) {
            var data :Array = actionData.split(sep);
            if (data.length > 5) {
                // if it's a newstyle portal, the last field is the target scene name,
                // which may have colons in it.
                data[5] = data.slice(5).join(sep);
                data.length = 6; // truncate
            }
            return data;

        } else {
            return [ actionData.substring(0, sepDex),
                     actionData.substring(sepDex + sep.length) ];
            // TODO: can we just do this? will the 2 mean "stick everything in the last argument"
            // or will it mean "ignore everything after the third colon"? the documentation of
            // course does not say...
//             return actionData.split(":", 2);
        }
    }

    /**
     * Sets whether this furniture is in 'perspective' mode.
     * TODO: support floor/ceiling perspectivization
     */
    public function setPerspective (perspective :Boolean) :void
    {
        //setLayoutInfo(PERSPECTIVE_FLAG, perspective);
    }

    /**
     * Is this furniture being perspectivized?
     */
    public function isPerspective () :Boolean
    {
        return false;
        //return isLayoutInfo(PERSPECTIVE_FLAG);
    }

    /**
     * Set whether or not this furni doesn't scale.
     */
    public function setNoScale (noscale :Boolean) :void
    {
        setLayoutInfo(NOSCALE_FLAG, noscale);
    }

    /**
     * Is this furniture non-scaling?
     */
    public function isNoScale () :Boolean
    {
        return isLayoutInfo(NOSCALE_FLAG);
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return id;
    }

    // from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is FurniData) && (other as FurniData).id == this.id;
    }

    // from interface Hashable
    public function hashCode () :int
    {
        return id;
    }

    /**
     * @return true if the other FurniData is identical.
     */
    public function equivalent (that :FurniData) :Boolean
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
            Util.equals(this.actionData, that.actionData);
    }

    public function toString () :String
    {
        var s :String = "Furni[id=" + id + ", itemType=" + itemType;
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

    /** Overwrites this instance's fields with a shallow copy of the other object. */
    protected function copyFrom (that :FurniData) :void
    {
        this.id = that.id;
        this.itemType = that.itemType;
        this.itemId = that.itemId;
        this.media = that.media;
        this.loc = that.loc;
        this.layoutInfo = that.layoutInfo;
        this.scaleX = that.scaleX;
        this.scaleY = that.scaleY;
        this.rotation = that.rotation;
        this.hotSpotX = that.hotSpotX;
        this.hotSpotY = that.hotSpotY;
        this.actionType = that.actionType;
        this.actionData = that.actionData;
    }

    // from interface Cloneable
    public function clone () :Object
    {
        // just a shallow copy at present
        var that :FurniData = (ClassUtil.newInstance(this) as FurniData);
        that.copyFrom(this);
        return that;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeShort(id);
        out.writeByte(itemType);
        out.writeInt(itemId);
        out.writeObject(media);
        out.writeObject(loc);
        out.writeByte(layoutInfo);
        out.writeFloat(scaleX);
        out.writeFloat(scaleY);
        out.writeFloat(rotation);
        out.writeShort(hotSpotX);
        out.writeShort(hotSpotY);
        out.writeByte(actionType);
        out.writeField(actionData);
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readShort();
        itemType = ins.readByte();
        itemId = ins.readInt();
        media = MediaDesc(ins.readObject());
        loc = MsoyLocation(ins.readObject());
        layoutInfo = ins.readByte();
        scaleX = ins.readFloat();
        scaleY = ins.readFloat();
        rotation = ins.readFloat();
        hotSpotX = ins.readShort();
        hotSpotY = ins.readShort();
        actionType = ins.readByte();
        actionData = (ins.readField(String) as String);
    }

    /**
     * Set a layoutInfo flag on or off.
     */
    protected function setLayoutInfo (flag :int, on :Boolean) :void
    {
        if (on) {
            layoutInfo |= flag;
        } else {
            layoutInfo &= ~flag;
        }
    }

    /**
     * Test a layoutInfo flag.
     */
    protected function isLayoutInfo (flag :int) :Boolean
    {
        return (layoutInfo & flag) != 0;
    }

    /** layoutInfo bitmask flag constant. Indicates if the furni is non-scaling. */
    protected static const NOSCALE_FLAG :int = (1 << 0);

    /** layoutInfo bitmask flag constant. Indicates if the furni is perspectivized. */
//    protected static const HORZ_PERSPECTIVE_FLAG :int = (1 << 1);

    /** layoutInfo bitmask flag constant. Indicates if the furni is perspectivized. */
//    protected static const VERT_PERSPECTIVE_FLAG :int = (1 << 2);
}
}
