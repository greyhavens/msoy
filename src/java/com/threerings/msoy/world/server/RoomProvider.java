//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

/**
 * Defines the server-side of the {@link RoomService}.
 */
public interface RoomProvider extends InvocationProvider
{
    /**
     * Handles a {@link RoomService#changeLocation} request.
     */
    public void changeLocation (ClientObject caller, ItemIdent arg1, Location arg2);

    /**
     * Handles a {@link RoomService#despawnMob} request.
     */
    public void despawnMob (ClientObject caller, int arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#editRoom} request.
     */
    public void editRoom (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#purchaseRoom} request.
     */
    public void purchaseRoom (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#requestControl} request.
     */
    public void requestControl (ClientObject caller, ItemIdent arg1);

    /**
     * Handles a {@link RoomService#sendSpriteMessage} request.
     */
    public void sendSpriteMessage (ClientObject caller, ItemIdent arg1, String arg2, byte[] arg3, boolean arg4);

    /**
     * Handles a {@link RoomService#sendSpriteSignal} request.
     */
    public void sendSpriteSignal (ClientObject caller, String arg1, byte[] arg2);

    /**
     * Handles a {@link RoomService#setActorState} request.
     */
    public void setActorState (ClientObject caller, ItemIdent arg1, int arg2, String arg3);

    /**
     * Handles a {@link RoomService#setRoomProperty} request.
     */
    public void setRoomProperty (ClientObject caller, RoomPropertyEntry arg1);

    /**
     * Handles a {@link RoomService#spawnMob} request.
     */
    public void spawnMob (ClientObject caller, int arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#updateMemory} request.
     */
    public void updateMemory (ClientObject caller, EntityMemoryEntry arg1);

    /**
     * Handles a {@link RoomService#updateRoom} request.
     */
    public void updateRoom (ClientObject caller, SceneUpdate[] arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
