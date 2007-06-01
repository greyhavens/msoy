//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.util.ActionScript;

/**
 * Notification that gets sent to a user whenever one of their friends logs on or off.
 */
public class FriendStatusChangeNotification extends Notification
{
    /** Friend's credentials. */
    public MemberName friend;

    /** True if the friend just logged on, false if the friend just logged off. */
    public boolean loggedOn;

    /** Creates a new notification instance. */
    @ActionScript(omit=true)
    public FriendStatusChangeNotification (MemberName friend, Boolean loggedOn)
    {
        this.friend = friend;
        this.loggedOn = loggedOn;
    }

    /** A blank constructor used during unserialization. */
    public FriendStatusChangeNotification ()
    {
    }
}
