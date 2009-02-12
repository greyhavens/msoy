//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.data.AuthName;

/**
 * Identifies the auth-username of a game authentication request.
 */
public class GameAuthName extends AuthName
{
    /** Creates a name for the member with the supplied account name and member id. */
    public GameAuthName (String accountName, int memberId)
    {
        super(accountName, memberId);
    }

    /** Used for unserializing. */
    public GameAuthName ()
    {
    }
}
