//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information displayed on a game's detail page.
 */
public class GameDetail
    implements IsSerializable
{
    /** The maximum allowed length for game instructions. */
    public static final int MAX_INSTRUCTIONS_LENGTH = 16384;

    /** The game for which we contain metadata. */
    public int gameId;

    /** This game's main metadata. */
    public GameInfo info;

    /** The creator supplied instructions for this game. */
    public String instructions;

    /** This game's play metrics. */
    public GameMetrics metrics;

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game. */
    public int maxPlayers;

    /** The rating given to this game by the requester, if any. */
    public byte memberRating;
}
