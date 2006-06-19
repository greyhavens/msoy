package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneModel;

import com.threerings.msoy.data.MediaData;

public class MsoySceneModel extends SceneModel
{
    /** The type of scene. */
    public var type :String;

    /** The pixel width of the room. */
    public var width :int;

    /** The background image of the scene. */
    public var background :MediaData;

    /** The music to play in the background. */
    public var music :MediaData;

    /** The furniture in the scene. */
    public var furnis :TypedArray;

    /**
     * Add a piece of furniture to this model.
     */
    public function addFurni (furni :FurniData) :void
    {
        furnis.push(furni);
    }

    /**
     * Remove a piece of furniture to this model.
     */
    public function removeFurni (furni :FurniData) :void
    {
        for (var ii :int = 0; ii < furnis.length; ii++) {
            if (furni.equals(furnis[ii])) {
                furnis.splice(ii, 1);
                return;
            }
        }
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(type);
        out.writeShort(width);
        out.writeObject(background);
        out.writeObject(music);
        out.writeObject(furnis);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = (ins.readField(String) as String);
        width = ins.readShort();
        background = (ins.readObject() as MediaData);
        music = (ins.readObject() as MediaData);
        furnis = (ins.readObject() as TypedArray);
    }

    public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", type=" + type + "]";
    }
}
}
