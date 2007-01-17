//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.data.MemoryEntry;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService
{
    /**
     * Requests that the specified item be assigned a controller. Other distributed state modifying
     * services will automatically assign a controller to an uncontrolled item the first time they
     * are requested, but if an entity simply wishes to start ticking itself locally, it must first
     * request control to ensure that the right client handles the ticking.
     */
    public void requestController (Client client, ItemIdent item, ConfirmListener listener);

    /**
     * Requests to trigger an event on the specified item or globally.
     *
     * @param item the identifier of the item on which to trigger the event, or null if it should
     * be delivered to all items.
     * @param event the event to trigger.
     */
    public void triggerEvent (Client client, ItemIdent item, String event, byte[] arg);

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

    /**
     * Issues a request to update the current scene location of the specified item. This is called
     * by Pets and other MOBs that want to move around the room.
     */
    public void changeLocation (Client client, ItemIdent item, Location newloc);
}
