package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.data.MediaData;

/**
 * Encodes a scene update that updates the attributes in the MsoySceneModel.
 * Note that this contains all attributes, even ones that have not changed.
 * In other words, a field being null doesn't mean that the field
 * isn't updated, it means the new value should be null.
 */
public class SceneAttrsUpdate extends SceneUpdate
{
    /** The new type. */
    public var type :String;

    /** The new width. */
    public var width :int;

    /** The new background. */
    public var background :MediaData;

    /** The new music. */
    public var music :MediaData;

    override public function apply (model :SceneModel) :void
    {
        super.apply(model);

        var mmodel :MsoySceneModel = (model as MsoySceneModel);
        mmodel.type = type;
        mmodel.width = width;
        mmodel.background = background;
        mmodel.music = music;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(type);
        out.writeShort(width);
        out.writeObject(background);
        out.writeObject(music);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = (ins.readField(String) as String);
        width = ins.readShort();
        background = (ins.readObject() as MediaData);
        music = (ins.readObject() as MediaData);
    }
}
}
