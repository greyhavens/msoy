//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
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

    /** The number of "player games" played (games times the number of players in each game). */
    public int playerGames;

    /** The total number of player minutes spent playing this game. */
    public int playerMinutes;

    /** This game's current abuse factor. */
    public float abuseFactor;

    /** The player minutes count of our last abuse recalculation. */
    public int lastAbuseRecalc;

    // TODO: all sorts of other fancy shit

    /**
     * Returns the listed game if we have one, the source if not.
     */
    public Game getGame ()
    {
        return (listedItem == null) ? sourceItem : listedItem;
    }

    /**
     * Clears out data that we don't want to send to non-admins.
     */
    public void clearNonAdminData ()
    {
        abuseFactor = 0;
        lastAbuseRecalc = 0;
    }
}
