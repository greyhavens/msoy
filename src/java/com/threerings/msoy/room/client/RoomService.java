//
// $Id$

package com.threerings.msoy.room.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.data.MobInfo;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService<ClientObject>
{
    /**
     * Requests that the specified item be assigned a controller. Other distributed state modifying
     * services will automatically assign a controller to an uncontrolled item the first time they
     * are requested, but if an entity simply wishes to start ticking itself locally, it must first
     * request control to ensure that the right client handles the ticking.
     */
    void requestControl (ItemIdent item);

    /**
     * Requests to send a sprite message.
     *
     * @param item the identifier of the item on which to trigger the event, or null if it should
     * be delivered to all items.
     * @param name the message name.
     * @param arg the data
     * @param isAction if the message is a "action".
     */
    void sendSpriteMessage (ItemIdent item, String name, byte[] arg,
                            boolean isAction);

    /**
     * Requests to send a sprite signal.
     *
     * @param name the message name.
     * @param arg the data
     */
    void sendSpriteSignal (String name, byte[] arg);

    /**
     * Requests to update an actor's state.
     */
    void setActorState (ItemIdent item, int actorOid, String state);

    /**
     * Requests to add or remove a song from the room.
     */
    void addOrRemoveSong (int audioId, boolean add, ConfirmListener listener);

    /**
     * Request to reorder a DJ's playlist.
     */
    void setTrackIndex (int audioId, int index);

    /**
     * A player's rating of the current track.
     */
    void rateTrack (int audioId, boolean like);

    /**
     * If the player was DJ-ing, remove them.
     */
    void quitDjing ();

    /**
     * Requests to remove another DJ from rotation.
     */
    void bootDj (int memberId, InvocationListener listener);

    /**
     * For managers, request to jump to a particular song in the playlist.
     */
    void jumpToSong (int audioId, ConfirmListener listener);

    /**
     * A callback from a client to indicate that a song has ended and the playlist
     * should move to the next song.
     */
    void songEnded (int playCount);

    /**
     * Requests to edit the client's current room.
     *
     * @param listener will be informed with an array of items in the room.
     */
    void editRoom (ResultListener listener);

    /**
     * Requests to apply the specified scene update to the room.
     */
    void updateRoom (SceneUpdate update, InvocationListener listener);

    /**
     * Requests to publish this room to the rest of friends and the rest of Whirled.
     */
    void publishRoom (InvocationListener listener);

    /**
     * Issues a request to update the memory of the specified entity (which is associated with a
     * particular item).
     */
    void updateMemory (
        ItemIdent ident, String key, byte[] newValue, ResultListener listener);

    /**
     * Issues a request to update the current scene location of the specified item. This is called
     * by Pets and other MOBs that want to move around the room.
     */
    void changeLocation (ItemIdent item, Location newloc);

    /**
     * Requests the placement of a MOB in the current scene location.
     *
     * @see MobInfo
     */
    void spawnMob (int gameId, String mobId, String mobName, Location startLoc,
                   InvocationListener listener);

    /**
     * Requests a mob be moved to a new location.
     */
    void moveMob (int gameId, String mobId, Location newLoc,
                  InvocationListener listener);

    /**
     * Requests the removal of a MOB from the current scene location.
     */
    void despawnMob (int gameId, String mobId, InvocationListener listener);

    /**
     * Requests to assign this user rating to the room. Returns a {@link RatingResult}.
     */
    void rateRoom (byte rating, InvocationListener listener);

    /**
     * Requests to send a postcard email containing a snapshot of this room.
     *
     * @param snapURL the URL of the snapshot or null to send the canonical snapshot.
     */
    void sendPostcard (String[] recips, String subject, String caption,
                       String snapURL, ConfirmListener listener);

}
