//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.orth.data.AuthName;

/**
 * Identifies the auth-username of a game authentication request.
 */
@com.threerings.util.ActionScript(omit=true)
public class GameAuthName extends AuthName
{
    /**
     * Creates an instance that can be used as a DSet key.
     */
    public static GameAuthName makeKey (int memberId)
    {
        return new GameAuthName("", memberId);
    }

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
