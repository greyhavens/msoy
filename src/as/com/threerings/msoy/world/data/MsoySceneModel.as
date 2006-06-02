package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.data.SceneModel;

import com.threerings.msoy.data.MediaData;

public class MsoySceneModel extends SceneModel
{
    /** The type of scene. */
    public var type :String;

    /** The background image of the scene. */
    public var background :MediaData;

    // documentation inherited
    public override function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(type);
        out.writeObject(background);
    }

    // documentation inherited
    public override function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = (ins.readField(String) as String);
        background = (ins.readObject() as MediaData);
    }

    public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", type=" + type + "]";
    }
}
}
