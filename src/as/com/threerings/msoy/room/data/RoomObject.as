//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.Iterator;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.RoomPropertiesEntry;

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

    /** The field name of the <code>propertySpaces</code> field. */
    public static const PROPERTY_SPACES :String = "propertySpaces";
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

    /** Contains the memories for all entities in this room. */
    public var memories :DSet = new DSet();
    EntityMemoryEntry; // reference to force linkage

    /** Contains mappings for all controlled entities in this room. */
    public var controllers :DSet = new DSet();
    EntityControl; // reference to force linkage

    /** The property spaces associated with this room. */
    public var propertySpaces :DSet = new DSet();
    RoomPropertiesEntry; // reference to force linkage

    /**
     * Finds the info of an occupant who is also a member and has a given member id. Performs the
     * same function as <code>getOccupantInfo(new MemberName("", memberId))</code>, but is more
     * convenient to call and performs better.
     */
    public function getMemberInfo (memberId :int) :MemberInfo
    {
        var itr :Iterator = occupantInfo.iterator();
        while (itr.hasNext()) {
            var minfo :MemberInfo = (itr.next() as MemberInfo);
            if (minfo != null && minfo.getMemberId() == memberId) {
                return minfo;
            }
        }
        return null;
    }
    
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = RoomMarshaller(ins.readObject());
        memories = DSet(ins.readObject());
        controllers = DSet(ins.readObject());
        propertySpaces = DSet(ins.readObject());
    }
}
}
