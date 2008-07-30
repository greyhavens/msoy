//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.GameState;

/**
 * The data shared between server, clients and agent for an AVR game.
 */
public class AVRGameObject extends PlaceObject
{
    /** Used on the server to listen to subscriber count changes to an avr game object. */
    public interface SubscriberListener
    {
        /** Called when the number of subscribers has changed. */
        void subscriberCountChanged (AVRGameObject target);
    }

    /** The identifier for a MessageEvent containing a user message. */
    public static final String USER_MESSAGE = "Umsg";

    /** The identifier for a MessageEvent containing ticker notifications. */
    public static final String TICKER = "Utick";

    /** A message dispatched to each player's client object when coins are awarded. */
    public static final String COINS_AWARDED_MESSAGE = "FlowAwarded";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gameMedia</code> field. */
    public static final String GAME_MEDIA = "gameMedia";

    /** The field name of the <code>state</code> field. */
    public static final String STATE = "state";

    /** The field name of the <code>playerLocs</code> field. */
    public static final String PLAYER_LOCS = "playerLocs";

    /** The field name of the <code>avrgService</code> field. */
    public static final String AVRG_SERVICE = "avrgService";
    // AUTO-GENERATED: FIELDS END

    /** The defining media of the AVRGame. */
    public MediaDesc gameMedia;

    /** Contains the game's memories. */
    public DSet<GameState> state = new DSet<GameState>();

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public DSet<PlayerLocation> playerLocs = new DSet<PlayerLocation>();

    /** Used to communicate with the AVRGameManager. */
    public AVRGameMarshaller avrgService;

    /** If set on the server, will be called with subscriber updates. */
    public transient SubscriberListener subscriberListener;

    /**
     * Expose our subscriber count.
     */
    public int getSubscriberCount ()
    {
        return _scount;
    }

    @Override // from DObject
    public void addSubscriber (Subscriber sub)
    {
        super.addSubscriber(sub);
        if (subscriberListener != null) {
            subscriberListener.subscriberCountChanged(this);
        }
    }

    @Override // from DObject
    public void removeSubscriber (Subscriber sub)
    {
        super.removeSubscriber(sub);
        if (subscriberListener != null) {
            subscriberListener.subscriberCountChanged(this);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>gameMedia</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameMedia (MediaDesc value)
    {
        MediaDesc ovalue = this.gameMedia;
        requestAttributeChange(
            GAME_MEDIA, value, ovalue);
        this.gameMedia = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>state</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToState (GameState elem)
    {
        requestEntryAdd(STATE, state, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>state</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromState (Comparable key)
    {
        requestEntryRemove(STATE, state, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>state</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateState (GameState elem)
    {
        requestEntryUpdate(STATE, state, elem);
    }

    /**
     * Requests that the <code>state</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setState (DSet<GameState> value)
    {
        requestAttributeChange(STATE, value, this.state);
        @SuppressWarnings("unchecked") DSet<GameState> clone =
            (value == null) ? null : value.typedClone();
        this.state = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>playerLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPlayerLocs (PlayerLocation elem)
    {
        requestEntryAdd(PLAYER_LOCS, playerLocs, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>playerLocs</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPlayerLocs (Comparable key)
    {
        requestEntryRemove(PLAYER_LOCS, playerLocs, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>playerLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePlayerLocs (PlayerLocation elem)
    {
        requestEntryUpdate(PLAYER_LOCS, playerLocs, elem);
    }

    /**
     * Requests that the <code>playerLocs</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPlayerLocs (DSet<PlayerLocation> value)
    {
        requestAttributeChange(PLAYER_LOCS, value, this.playerLocs);
        @SuppressWarnings("unchecked") DSet<PlayerLocation> clone =
            (value == null) ? null : value.typedClone();
        this.playerLocs = clone;
    }

    /**
     * Requests that the <code>avrgService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setAvrgService (AVRGameMarshaller value)
    {
        AVRGameMarshaller ovalue = this.avrgService;
        requestAttributeChange(
            AVRG_SERVICE, value, ovalue);
        this.avrgService = value;
    }
    // AUTO-GENERATED: METHODS END
}
