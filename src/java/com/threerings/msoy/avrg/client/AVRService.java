//
// $Id$

package com.threerings.msoy.avrg.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.avrg.data.AVRGameConfig;

/**
 * A service for managing AVR (in-world) game life cycles.
 */
public interface AVRService extends InvocationService<ClientObject>
{
    /**
     * Used to communicate the response to a {@link #activateGame} request.
     */
    public static interface AVRGameJoinListener extends InvocationListener
    {
        /**
         * Notifies the caller that an AVRG was successfully joined.
         *
         * @param placeId the object id of the AVRG's place object
         * @param config metadata related to the newly occupied game
         */
        public void avrgJoined (int placeId, AVRGameConfig config);
    }
    /**
     * Requests to active an AVR Game.
     *
     * @param gameId the item id of a Game-type item.
     * @param listener a listener to return result to or notify on failure.
     */
    void activateGame (int gameId, AVRGameJoinListener listener);

    /**
     * Requests to deactivate the given AVR Game, which must be current to the player.
     */
    void deactivateGame (int gameId, ConfirmListener listener);
}
