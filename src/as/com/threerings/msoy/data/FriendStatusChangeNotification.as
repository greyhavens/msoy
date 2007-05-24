//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.data.all.MemberName;

/**
 * Notification that gets sent to a user whenever one of their friends logs on or off.
 */
public class FriendStatusChangeNotification extends Notification
{
    /** Friend's credentials. */
    public var friend :MemberName;

    /** True if the friend just logged on, false if the friend just logged off. */
    public var loggedOn :Boolean;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        friend = (ins.readObject() as MemberName);
        loggedOn = ins.readBoolean();
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(friend);
        out.writeBoolean(loggedOn);
    }
}
}
