//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class GameCredentials extends MsoyCredentials
{
    public function GameCredentials (name :Name = null)
    {
        super(name);
    }
}
}
