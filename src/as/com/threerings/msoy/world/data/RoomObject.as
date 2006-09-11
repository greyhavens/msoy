//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.spot.data.SpotSceneObject;

/**
 * Room stuff.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static const ROOM_SERVICE :String = "roomService";
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

//    // AUTO-GENERATED: METHODS START
//    /**
//     * Requests that the <code>roomService</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setRoomService (value :RoomMarshaller) :void
//    {
//        var ovalue :RoomMarshaller = this.roomService;
//        requestAttributeChange(
//            ROOM_SERVICE, value, ovalue);
//        this.roomService = value;
//    }
//    // AUTO-GENERATED: METHODS END
//
//    override public function writeObject (out :ObjectOutputStream) :void
//    {
//        super.writeObject(out);
//
//        out.writeObject(roomService);
//    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = (ins.readObject() as RoomMarshaller);
    }
}
}
