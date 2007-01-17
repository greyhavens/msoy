//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

/**
 * Contains the distributed state of a virtual world room.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static const ROOM_SERVICE :String = "roomService";

    /** The field name of the <code>memories</code> field. */
    public static const MEMORIES :String = "memories";

    /** The field name of the <code>controllers</code> field. */
    public static const CONTROLLERS :String = "controllers";
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

    /** Contains the memories for all entities in this room. */
    public var memories :DSet = new DSet();

    /** Contains mappings for all controlled entities in this room. */
    public var controllers :DSet = new DSet();

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = (ins.readObject() as RoomMarshaller);
        memories = (ins.readObject() as DSet);
        controllers = (ins.readObject() as DSet);
    }
}
}
