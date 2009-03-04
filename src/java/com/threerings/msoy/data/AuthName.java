//
// $Id$

package com.threerings.msoy.data;

import com.threerings.util.Name;

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

    protected int _memberId;
}
