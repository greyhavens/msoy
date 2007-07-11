//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.util.Comparable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet_Entry;

import mx.core.UIComponent;

/**
 * Notification from the server to the client. Instances of notifications are collected inside
 * a DSet on the member object. Once the client dealt with a notification, it must acknowledge
 * it via an appropriate call to the MemberService, which will remove the notification from
 * the member object.
 */
public /* abstract */ class Notification extends SimpleStreamableObject
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

    /**
     * Should this notification hang around until the user acknowledges it?
     */
    public function isPersistent () :Boolean
    {
        return true;
    }

    /**
     * Get the chat message used to announce this notification, or null.
     * WTF are you doing with a null announcement?
     *
     * All announcements will be translated using the "notify" bundle.
     * You can qualify the string if you want a different bundle.
     */
    public function getAnnouncement () :String
    {
        throw new Error("Abstract");
    }

    /**
     * Get the UIComponent used to display this notification.
     */
    public function getDisplay () :UIComponent
    {
        throw new Error("Abstract");
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
        throw new Error();
    }
}
}
