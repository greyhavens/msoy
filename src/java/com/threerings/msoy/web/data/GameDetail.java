//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

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

    /** The item listed in the catalog for this game. */
    public Game listedItem;

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

    public String getName ()
    {
        return (listedItem == null) ? sourceItem.name : listedItem.name;
    }

    public MediaDesc getThumbnailMedia ()
    {
        return (listedItem == null) ?
            sourceItem.getThumbnailMedia() : listedItem.getThumbnailMedia();
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
