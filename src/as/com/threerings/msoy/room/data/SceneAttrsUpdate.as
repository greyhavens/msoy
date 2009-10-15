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

    /** New playlist control info. */
    public var playlistControl :int;

    /** Full description of the new decor. */
    public var decor :Decor;

    /** The new entrance location. */
    public var entrance :MsoyLocation;

    /** The new background color. */
    public var backgroundColor :uint;

    override public function apply (model :SceneModel) :void
    {
        super.apply(model);

        var mmodel :MsoySceneModel = (model as MsoySceneModel);
        mmodel.name = name;
        mmodel.accessControl = accessControl;
        mmodel.playlistControl = playlistControl;
        mmodel.decor = decor;
        mmodel.entrance = entrance;
        mmodel.backgroundColor = backgroundColor;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(name);
        out.writeByte(accessControl);
        out.writeByte(playlistControl);
        out.writeObject(decor);
        out.writeObject(entrance);
        out.writeInt(backgroundColor);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        name = (ins.readField(String) as String);
        accessControl = ins.readByte();
        playlistControl = ins.readByte();
        decor = Decor(ins.readObject());
        entrance = MsoyLocation(ins.readObject());
        backgroundColor = ins.readInt();
    }
}
}
