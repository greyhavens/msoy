//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.msoy.data.all.MemberName;

/**
 * Notification that gets sent to a user when an invited friend accepts the invitation.
 */
public class FriendAcceptedInvitationNotification extends Notification
{
    /** Friend's credentials. */
    public var friend :MemberName;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        friend = (ins.readObject() as MemberName);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(friend);
    }
}
}
