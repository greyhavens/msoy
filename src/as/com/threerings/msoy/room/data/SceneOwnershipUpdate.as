//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.util.Name;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;

/**
 */
public class SceneOwnershipUpdate extends SceneUpdate
{
    /** The new owner type. */
    public var ownerType :int;

    /** The new owner id. */
    public var ownerId :int;

    /** The new owner name. */
    public var ownerName :Name;

    /** If true, the accessControl is set to ACCESS_OWNER_ONLY. */
    public var lockToOwner :Boolean;

    override public function apply (model :SceneModel) :void
    {
        super.apply(model);

        var mmodel :MsoySceneModel = (model as MsoySceneModel);
        mmodel.ownerType = ownerType;
        mmodel.ownerId = ownerId;
        mmodel.ownerName = ownerName;
        if (lockToOwner) {
            mmodel.accessControl = MsoySceneModel.ACCESS_OWNER_ONLY;
        }
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(ownerType);
        out.writeInt(ownerId);
        out.writeObject(ownerName);
        out.writeBoolean(lockToOwner);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        ownerType = ins.readByte();
        ownerId = ins.readInt();
        ownerName = Name(ins.readObject());
        lockToOwner = ins.readBoolean();
    }
}
}
