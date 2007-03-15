package com.threerings.msoy.world.data {

import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

public class DecorData
    implements Cloneable, Hashable, Streamable
{
    /** The id of this piece of decor. */
    public var id :int;

    /** Info about the media that represents this piece of decor. */
    public var media :MediaDesc;

    /** Room type. Controls how the background wallpaper image is handled. */
    public var type :int;
    
    /** Room height, in pixels. */
    public var height :int;
    
    /** Room width, in pixels. */
    public var width :int;

    /** Room depth, in pixels. */
    public var depth :int;

    /** Horizon position, in [0, 1]. */
    public var horizon :Number;


    // documentation inherited from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is DecorData) &&
            (other as DecorData).id == this.id;
    }

    // documentation inherited from interface Hashable
    public function hashCode () :int
    {
        return id;
    }

    /**
     * @return true if the other DecorData is identical.
     */
    public function equivalent (that :DecorData) :Boolean
    {
        return this.id == that.id &&
            this.media.equals(that.media) &&
            this.type == that.type &&
            this.height == that.height &&
            this.width == that.width &&
            this.depth == that.depth &&
            this.horizon == that.horizon;
    }

    public function toString () :String
    {
        var s :String = "Decor[id=" + id + ", type=" + type + "]";
        return s;
    }

    // documentation inherited from interface Cloneable
    public function clone () :Object
    {
        // just a shallow copy at present
        var that :DecorData = (ClassUtil.newInstance(this) as DecorData);
        that.id = this.id;
        that.media = this.media;
        that.type = this.type;
        that.height = this.height;
        that.width = this.width;
        that.depth = this.depth;
        that.horizon = this.horizon;
        return that;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeShort(id);
        out.writeObject(media);
        out.writeByte(type);
        out.writeShort(height);
        out.writeShort(width);
        out.writeShort(depth);
        out.writeFloat(horizon);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readShort();
        media = (ins.readObject() as MediaDesc);
        type = ins.readByte(type);
        height = ins.readShort(height);
        width = ins.readShort(width);
        depth = ins.readShort(depth);
        horizon = ins.readFloat(horizon);
    }
}
}
