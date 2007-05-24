//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Notification that gets sent to a user when an invited friend accepts the invitation.
 */
public class FriendAcceptedInvitationNotification extends Notification
{
    /** Invited friend's screen name. */
    public var inviteeDisplayName :String;

    /** Friend's email address, to which the invitation was sent. */
    public var invitationEmail :String;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        inviteeDisplayName = (ins.readField(String) as String);
        invitationEmail = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(inviteeDisplayName);
        out.writeField(invitationEmail);
    }
}
}
