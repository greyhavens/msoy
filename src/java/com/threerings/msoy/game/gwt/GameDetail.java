//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.gwt.MemberItemInfo;

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

    /** Contains member rating and favorite information about the game. */
    public MemberItemInfo member = new MemberItemInfo();

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game. */
    public int maxPlayers;
}
