//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains raw usage data on a particular game.
 */
public class GameDetail
    implements IsSerializable
{
    /** The id of the game in question. */
    public int gameId;

    /** The name of the creator of this game. */
    public MemberName creator;

    /** The item listed in the catalog for this game. */
    public Game listedItem;

    /** The member's rating for the listed catalog item, if they have one. */
    public byte memberRating;

    /** The source item maintained by the creator for this game. */
    public Game sourceItem;

    /** The creator supplied instructions for this game. */
    public String instructions;

    /** The number of "player games" played (games times the number of players in each game). */
    public int playerGames;

    /** The total number of player minutes spent playing this game. */
    public int playerMinutes;

    /** The minimum number of players for this game. */
    public int minPlayers;

    /** The maximum number of players for this game or Integer.MAX_VALUE if it's a party game. */
    public int maxPlayers;

    /**
     * Returns the listed game if we have one, the source if not.
     */
    public Game getGame ()
    {
        return (listedItem == null) ? sourceItem : listedItem;
    }
}
