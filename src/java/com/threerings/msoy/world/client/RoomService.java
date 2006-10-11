//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.whirled.data.SceneUpdate;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService
{
    /**
     * Requests to edit the client's current room.
     *
     * @param listener will be informed with an array of items in the room.
     */
    public void editRoom (Client client, ResultListener listener);

    /**
     * Request to apply the specified scene updates to the room.
     */
    public void updateRoom (
            Client client, SceneUpdate[] updates, InvocationListener listener);
}
