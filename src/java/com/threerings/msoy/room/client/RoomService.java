//
// $Id$

package com.threerings.msoy.room.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.MobInfo;

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
    void requestControl (Client client, ItemIdent item);

    /**
     * Requests to send a sprite message.
     *
     * @param item the identifier of the item on which to trigger the event, or null if it should
     * be delivered to all items.
     * @param name the message name.
     * @param arg the data
     * @param isAction if the message is a "action".
     */
    void sendSpriteMessage (Client client, ItemIdent item, String name, byte[] arg,
                            boolean isAction);

    /**
     * Requests to send a sprite signal.
     *
     * @param name the message name.
     * @param arg the data
     */
    void sendSpriteSignal (Client client, String name, byte[] arg);

    /**
     * Requests to update an actor's state.
     */
    void setActorState (Client client, ItemIdent item, int actorOid, String state);

    /**
     * Requests to edit the client's current room.
     *
     * @param listener will be informed with an array of items in the room.
     */
    void editRoom (Client client, ResultListener listener);

    /**
     * Requests to apply the specified scene update to the room.
     */
    void updateRoom (Client client, SceneUpdate update, InvocationListener listener);

    /**
     * Requests to purchase a new room.
     */
    void purchaseRoom (Client client, ResultListener listener);

    /**
     * Requests to publish this room to the rest of friends and the rest of Whirled.
     */
    void publishRoom (Client client, InvocationListener listener);

    /**
     * Issues a request to update the memory of the specified entity (which is associated with a
     * particular item).
     */
    void updateMemory (
        Client client, ItemIdent ident, String key, byte[] newValue, ResultListener listener);

    /**
     * Issues a request to update the current scene location of the specified item. This is called
     * by Pets and other MOBs that want to move around the room.
     */
    void changeLocation (Client client, ItemIdent item, Location newloc);

    /**
     * Requests the placement of a MOB in the current scene location.
     *
     * @see MobInfo
     */
    void spawnMob (Client caller, int gameId, String mobId, String mobName, Location startLoc,
                   InvocationListener listener);

    /**
     * Requests a mob be moved to a new location.
     */
    void moveMob (Client caller, int gameId, String mobId, Location newLoc,
                  InvocationListener listener);

    /**
     * Requests the removal of a MOB from the current scene location.
     *
     * @see MobInfo
     */
    void despawnMob (Client caller, int gameId, String mobId, InvocationListener listener);

    /**
     * Requests to assign this user rating to the room. Returns a {@link RatingResult}.
     */
    void rateRoom (Client caller, byte rating, InvocationListener listener);

    /**
     * Requests to send a postcard email containing a snapshot of this room.
     *
     * @param snapURL the URL of the snapshot or null to send the canonical snapshot.
     */
    void sendPostcard (Client caller, String[] recips, String subject, String caption,
                       String snapURL, ConfirmListener listener);
}
