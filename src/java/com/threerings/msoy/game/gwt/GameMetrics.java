//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a game's play metrics.
 */
public class GameMetrics
    implements IsSerializable
{
    /** The id of the game for which we're reporting metrics. */
    public int gameId;

    /** The total number of player games this game has accumulated. */
    public int gamesPlayed;

    /** The reported average duration of this game in seconds. */
    public int averageDuration;

    /** When this game last paid out coins. */
    public long lastPayout;
}
