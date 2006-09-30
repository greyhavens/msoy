package com.threerings.msoy.world.data {

import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.MediaDesc;

public class FurniData
    implements Cloneable, Hashable, Streamable
{
    /** An actionType indicating 'no action'. */
    public static const ACTION_NONE :int = 0;

    /** An actionType indicating that actionData is a URL. */
    public static const ACTION_URL :int = 1;

    /** An actionType indicating that actionData is a game item id. */
    public static const ACTION_GAME :int = 2;

//    /** The itemType of the item that was used to create this furni. */
//    public var itemType :int;

    /** The id of this piece of furni. */
    public var id :int;

    /** Info about the media that represents this piece of furni. */
    public var media :MediaDesc;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    /** A scale factor in the X direction. */
    public var scaleX :Number = 1;

    /** A scale factor in the Y direction. */
    public var scaleY :Number = 1;

    /** The type of action, determines how to use actionData. */
    public var actionType :int;

    /** The action, interpreted using actionType. */
    public var actionData :String;

    // documentation inherited from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is FurniData) &&
            (other as FurniData).id == this.id;
    }

    // documentation inherited from interface Hashable
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
            this.media.equals(that.media) &&
            this.loc.equals(that.loc) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY) &&
            (this.actionType == that.actionType) &&
            Util.equals(this.actionData, that.actionData);
    }

    // documentation inherited from interface Cloneable
    public function clone () :Object
    {
        // just a shallow copy at present
        var that :FurniData = (ClassUtil.newInstance(this) as FurniData);
        that.id = this.id;
        that.media = this.media;
        that.loc = this.loc;
        that.scaleX = this.scaleX;
        that.scaleY = this.scaleY;
        that.actionType = this.actionType;
        that.actionData = this.actionData;
        return that;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(id);
        out.writeObject(media);
        out.writeObject(loc);
        out.writeFloat(scaleX);
        out.writeFloat(scaleY);
        out.writeByte(actionType);
        out.writeField(actionData);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readInt();
        media = (ins.readObject() as MediaDesc);
        loc = (ins.readObject() as MsoyLocation);
        scaleX = ins.readFloat();
        scaleY = ins.readFloat();
        actionType = ins.readByte();
        actionData = (ins.readField(String) as String);
    }
}
}
