package com.threerings.msoy.world.data {

import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.Decor;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

public class DecorData extends FurniData
    implements Cloneable, Hashable, Streamable
{
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

    /**
     * Helper function: specifies that this decor data structure has already been
     * populated from a Decor item object.
     */
    public function isInitialized () :Boolean
    {
        return itemId != 0;
    }
    
    // documentation inherited from superinterface Equalable
    override public function equals (other :Object) :Boolean
    {
        return (other is DecorData) &&
            (other as DecorData).id == this.id;
    }

    // documentation inherited from interface Hashable
    override public function hashCode () :int
    {
        return id;
    }

    // documentation inherited from FurniData
    override public function equivalent (that :FurniData) :Boolean
    {
        if (! (that is DecorData)) {
            return false;
        } else {
            var data :DecorData = that as DecorData;
            return super.equivalent(that) &&
                this.type == data.type &&
                this.height == data.height &&
                this.width == data.width &&
                this.depth == data.depth &&
                this.horizon == data.horizon;
        }
    }

    override public function toString () :String
    {
        var s :String = "Decor[itemId=" + itemId + ", type=" + type + ", media=" + media + "]";
        return s;
    }

    // documentation inherited from FurniData
    override protected function copyFrom (that :FurniData) :void
    {
        super.copyFrom(that);
        if (that is DecorData) {
            var data :DecorData = that as DecorData;
            this.type = data.type;
            this.height = data.height;
            this.width = data.width;
            this.depth = data.depth;
            this.horizon = data.horizon;
        }
    }
    
    // documentation inherited from interface Cloneable
    override public function clone () :Object
    {
        // just a shallow copy at present
        var that :DecorData = (ClassUtil.newInstance(this) as DecorData);
        that.copyFrom(this);
        return that;
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeByte(type);
        out.writeShort(height);
        out.writeShort(width);
        out.writeShort(depth);
        out.writeFloat(horizon);
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        type = ins.readByte();
        height = ins.readShort();
        width = ins.readShort();
        depth = ins.readShort();
        horizon = ins.readFloat();
    }
}
}
