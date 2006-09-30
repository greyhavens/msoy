package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Encodes a scene update that updates the attributes in the MsoySceneModel.
 * Note that this contains all attributes, even ones that have not changed.
 * In other words, a field being null doesn't mean that the field
 * isn't updated, it means the new value should be null.
 */
public class SceneAttrsUpdate extends SceneUpdate
{
    /** The new name. */
    public var name :String;

    /** The new type. */
    public var type :int;

    /** The new depth. */
    public var depth :int;

    /** The new width. */
    public var width :int;

    /** The new horizon. */
    public var horizon :Number;

    override public function apply (model :SceneModel) :void
    {
        super.apply(model);

        var mmodel :MsoySceneModel = (model as MsoySceneModel);
        mmodel.name = name;
        mmodel.type = type;
        mmodel.depth = depth;
        mmodel.width = width;
        mmodel.horizon = horizon;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(name);
        out.writeByte(type);
        out.writeShort(depth);
        out.writeShort(width);
        out.writeFloat(horizon);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = (ins.readField(String) as String);
        type = ins.readByte();
        depth = ins.readShort();
        width = ins.readShort();
        horizon = ins.readFloat();
    }
}
}
