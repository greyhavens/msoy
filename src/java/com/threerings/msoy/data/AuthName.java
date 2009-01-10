//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.Name;

import com.threerings.msoy.data.all.MemberName;

/**
 * Represents the authentication username for our various sessions (world, game, party).
 */
public class AuthName extends Name
{
    /** Creates a name for the member with the supplied account name and member id. */
    public AuthName (String accountName, int memberId)
    {
        super(accountName);
        _memberId = memberId;
    }

    /** Used when unserializing. */
    public AuthName ()
    {
    }

    /** Returns this member's unique id. */
    public int getMemberId ()
    {
        return _memberId;
    }

    /** Returns true if this name represents a guest member or a viewer. */
    public boolean isGuest ()
    {
        return MemberName.isGuest(_memberId);
    }

    protected int _memberId;
}
