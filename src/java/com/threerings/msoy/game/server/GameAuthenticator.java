//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Singleton;

import com.threerings.msoy.data.AuthName;
import com.threerings.msoy.server.AuxAuthenticator;

import com.threerings.msoy.game.data.GameAuthName;
import com.threerings.msoy.game.data.GameCredentials;

/**
 * Handles authentication on an MSOY Game server.
 */
@Singleton
public class GameAuthenticator extends AuxAuthenticator<GameCredentials>
{
    protected GameAuthenticator ()
    {
        super(GameCredentials.class);
    }

    @Override // from AuxAuthenticator
    protected AuthName createName (String accountName, int memberId)
    {
        return new GameAuthName(accountName, memberId);
    }
}
