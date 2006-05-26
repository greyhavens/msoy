package com.threerings.msoy.world.data {

import com.threerings.io.TypedArray;

import com.threerings.whirled.data.AuxModel;
import com.threerings.whirled.data.SceneModel;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

public class MsoyFurniSceneModel
    implements AuxModel
{
    /** An array of data about furniture in the scene. */
    public var furniData :TypedArray;

    // documentation inherited from interface AuxModel (Streamable)
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(furniData);
    }

    // documentation inherited from interface AuxModel (Streamable)
    public function readObject (ins :ObjectInputStream) :void
    {
        furniData = (ins.readObject() as TypedArray);
    }

    // documentation inherited from interface AuxModel (Cloneable)
    public function clone () :Object
    {
        var that :MsoyFurniSceneModel = new MsoyFurniSceneModel();
        that.furniData = this.furniData;
        return that;
    }
}
}
