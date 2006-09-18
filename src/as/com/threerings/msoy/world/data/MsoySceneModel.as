package com.threerings.msoy.world.data {

import com.threerings.util.ArrayUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneModel;

import com.threerings.msoy.item.web.MediaDesc;

public class MsoySceneModel extends SceneModel
{
    /** A type constant indicating a normal room where defaultly
     * draw some walls. */
    public static const DRAWN_ROOM :int = 0;

    /** A type constant indicating a room where the background image should
     * be drawn covering everything, but layered behind everything else such
     * that the background image IS the scene to the viewer. */
    public static const IMAGE_OVERLAY :int = 1;

    /** The type of scene. */
    public var type :int;

    /** The memberId of the owner of this scene. */
    public var ownerId :int;

    /** The "pixel" depth of the room. */
    public var depth :int;

    /** The pixel width of the room. */
    public var width :int;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public var horizon :Number;

    /** The background image of the scene. */
    public var background :MediaDesc;

    /** The music to play in the background. */
    public var music :MediaDesc;

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
        model.depth = depth;
        model.width = width;
        model.horizon = horizon;
        model.background = background;
        model.music = music;
        model.furnis = (furnis.clone() as TypedArray);

        return model;
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(type);
        out.writeInt(ownerId);
        out.writeShort(depth);
        out.writeShort(width);
        out.writeFloat(horizon);
        out.writeObject(background);
        out.writeObject(music);
        out.writeObject(furnis);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = ins.readByte();
        ownerId = ins.readInt();
        depth = ins.readShort();
        width = ins.readShort();
        horizon = ins.readFloat();
        background = (ins.readObject() as MediaDesc);
        music = (ins.readObject() as MediaDesc);
        furnis = (ins.readObject() as TypedArray);
    }

    public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", type=" + type + "]";
    }
}
}
