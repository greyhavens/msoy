//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.game.client.LobbyService;

/**
 * Constants used by the {@link LobbyService#playNow}.
 */
public interface LobbyCodes extends InvocationCodes
{
    /** Resolves the lobby, and starts a single-player game. */
    public static final int PLAY_NOW_SINGLE = 0;

    /** Resolves the lobby, and starts a multiplayer game with anyone. */
    public static final int PLAY_NOW_ANYONE = 1;

    /**
     * Resolves the lobby, and if the game is single-player only, starts a new game.
     * Otherwise just shows the resolved lobby.
     */
    public static final int PLAY_NOW_IF_SINGLE = 2;
}
