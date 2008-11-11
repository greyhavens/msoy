//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.gwt.MemberItemInfo;

/**
 * Contains information displayed on a game's detail page.
 */
public class GameDetail
    implements IsSerializable
{
    /** The maximum allowed length for game instructions. */
    public static final int MAX_INSTRUCTIONS_LENGTH = 16384;

    /** The id of the game in question. */
    public int gameId;

    /** The name of the creator of this game. */
    public MemberName creator;

    /** The item id of this game's source (development version) item. */
    public int sourceItemId;

    /** The item that defines this game's properties (this is the source item if the development
     * version was requested or the listed item if the production version was requested. */
    public Game item;

    /** The creator supplied instructions for this game. */
    public String instructions;

    /** The total number of player games this game has accumulated. */
    public int gamesPlayed;

    /** The reported average duration of this game in seconds. */
    public int averageDuration;

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game or Integer.MAX_VALUE if it's a party game. */
    public int maxPlayers;

    /** The number of people playing this game right now. */
    public int playingNow;

    /** Contains member rating and favorite information about the game. */
    public MemberItemInfo memberItemInfo = new MemberItemInfo();

    /**
     * Returns true if this is a party game, false otherwise.
     */
    public boolean isPartyGame ()
    {
        return maxPlayers == Integer.MAX_VALUE;
    }

    /**
     * Returns true if the specified member is the creator of this game.
     */
    public boolean isCreator (int memberId)
    {
        return (creator != null) && creator.getMemberId() == memberId;
    }
}
