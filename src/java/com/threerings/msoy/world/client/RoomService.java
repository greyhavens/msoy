//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.world.data.EntityIdent;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.RoomCodes;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService
{
    /**
     * Requests to trigger an event on the specified entity or globally.
     *
     * @param entity the identifier of the entity on which to trigger the event, or null if it
     * should be delivered to all entities.
     * @param event the event to trigger.
     */
    public void triggerEvent (Client client, EntityIdent entity, String event);

    /**
     * Requests to edit the client's current room.
     *
     * @param listener will be informed with an array of items in the room.
     */
    public void editRoom (Client client, ResultListener listener);

    /**
     * Request to apply the specified scene updates to the room.
     */
    public void updateRoom (Client client, SceneUpdate[] updates, InvocationListener listener);

    /**
     * Issues a request to update the memory of the specified entity (which is associated with a
     * particular item).
     */
    public void updateMemory (Client client, MemoryEntry entry);
}
