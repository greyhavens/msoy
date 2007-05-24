//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet_Entry;
import com.threerings.util.Comparable;

/**
 * Notification from the server to the client. Instances of notifications are collected inside
 * a DSet on the member object. Once the client dealt with a notification, it must acknowledge
 * it via an appropriate call to the MemberService, which will remove the notification from
 * the member object.
 */
public class Notification extends SimpleStreamableObject
    implements Comparable, DSet_Entry
{
    /**
     * Id of the notification. This field will be set by the MemberObject to which
     * the notification is sent. Within the scope of that object, each notification
     * will have a unique id.
     */
    public var id :int;

    // from Comparable
    public function compareTo (arg1 :Object) :int
    {
        var that :Notification = (arg1 as Notification);
        return this.id - that.id;
    }

    // from DSet.Entry
    public function getKey () :Object
    {
        return this.id;
    }

    // from SimpleStreamableObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        id = ins.readInt();
    }

    // from SimpleStreamableObject
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(id);
    }
}
}
