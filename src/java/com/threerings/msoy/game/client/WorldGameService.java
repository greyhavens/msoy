//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

/**
 * Game-related services provided by the World server.
 */
public interface WorldGameService extends InvocationService<ClientObject>
{
    /** Reports the server and port on which to connect to a requested game's server. */
    public static interface LocationListener extends InvocationListener
    {
        void gameLocated (String host, int port, boolean inWorld);
    }

    /**
     * Locates (potentially resolving in the process) the server on which a game is hosted.
     */
    void locateGame (int gameId, LocationListener listener);

    /**
     * Issues a request to the specified friends to invite them to join the requesting player's
     * game. If the game is still being match-made, they'll join the table in question, if it's in
     * play, they'll join the game if possible and watch otherwise.
     */
    void inviteFriends (int gameId, int[] friendIds);

    /**
     * Returns a list of TablesWaiting objects, detailing games that actually have people
     * waiting to play.
     */
    void getTablesWaiting (ResultListener listener);
}
