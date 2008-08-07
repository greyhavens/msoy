//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.item.data.all.Decor;

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

    /** New access control info. */
    public var accessControl :int;
    
    /** Full description of the new decor. */
    public var decor :Decor;
    
    /** Background audio parameters. */
    public var audioData :AudioData;

    /** The new entrance location. */
    public var entrance :MsoyLocation;

    override public function apply (model :SceneModel) :void
    {
        super.apply(model);

        var mmodel :MsoySceneModel = (model as MsoySceneModel);
        mmodel.name = name;
        mmodel.accessControl = accessControl;
        mmodel.decor = decor;
        mmodel.audioData = (audioData.clone() as AudioData);
        mmodel.entrance = entrance;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(name);
        out.writeByte(accessControl);
        out.writeObject(decor);
        out.writeObject(audioData);
        out.writeObject(entrance);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = (ins.readField(String) as String);
        accessControl = ins.readByte();
        decor = (ins.readObject() as Decor);
        audioData = (ins.readObject() as AudioData);
        entrance = (ins.readObject() as MsoyLocation);
    }
}
}
