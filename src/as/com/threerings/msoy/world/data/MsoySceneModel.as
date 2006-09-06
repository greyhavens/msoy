package com.threerings.msoy.world.data {

import com.threerings.util.ArrayUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneModel;

import com.threerings.msoy.data.MediaData;

public class MsoySceneModel extends SceneModel
{
    /** The type of scene. */
    public var type :String;

    /** The memberId of the owner of this scene. */
    public var ownerId :int;

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
        ArrayUtil.removeFirst(furnis, furni);
    }

    /**
     * Get the next available furni id.
     */
    public function getNextFurniId () :int
    {
        var length :int = (furnis == null) ? 0 : furnis.length;
        for (var ii :int = 1; ii < 5000; ii++) {
            var found :Boolean = false;
            for (var idx :int = 0; idx < length; idx++) {
                if ((furnis[idx] as FurniData).id == ii) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ii;
            }
        }
        return -1;
    }

    override public function clone () :Object
    {
        var model :MsoySceneModel = (super.clone() as MsoySceneModel);

        model.type = type;
        model.ownerId = ownerId;
        model.width = width;
        model.background = background;
        model.music = music;
        model.furnis = (furnis.clone() as TypedArray);

        return model;
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(type);
        out.writeInt(ownerId);
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
        ownerId = ins.readInt();
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
