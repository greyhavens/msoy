//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.presents.data.InvocationCodes;

/**
 * Constants used by the {@link LobbyService#playNow}.
 */
public class LobbyCodes extends InvocationCodes
{
    /** Resolves the lobby, and starts a single-player game. */
    public static const PLAY_NOW_SINGLE :int = 0;

    /** Resolves the lobby, and starts a multiplayer game with anyone. */
    public static const PLAY_NOW_ANYONE :int = 1;

    /** 
     * Resolves the lobby, and if the game is single-player only, starts a new game.
     * Otherwise just shows the resolved lobby. 
     */
    public static const PLAY_NOW_IF_SINGLE :int = 2;
}
}
