//
// $Id$

package com.threerings.msoy.avrg.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.whirled.game.data.ContentMarshaller;
import com.whirled.game.data.GameData;
import com.whirled.game.data.GameDataObject;
import com.whirled.game.data.PrizeMarshaller;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameMessageMarshaller;
import com.whirled.game.server.PropertySpaceHelper;

import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * The data shared between server, clients and agent for an AVR game.
 */
public class AVRGameObject extends PlaceObject
    implements PropertySpaceObject, GameDataObject, PartyPlaceObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static final String USER_MESSAGE = "Umsg";

    /** The identifier for a MessageEvent containing a user message. */
    public static final String USER_MESSAGE_EXCLUDE_AGENT = "UmsgEA";

    /** The identifier for a MessageEvent containing ticker notifications. */
    public static final String TICKER = "Utick";

    /** A message dispatched to each player's client object when a task is completed. */
    public static final String TASK_COMPLETED_MESSAGE = "TaskCompleted";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerLocs</code> field. */
    public static final String PLAYER_LOCS = "playerLocs";

    /** The field name of the <code>parties</code> field. */
    public static final String PARTIES = "parties";

    /** The field name of the <code>partyLeaders</code> field. */
    public static final String PARTY_LEADERS = "partyLeaders";

    /** The field name of the <code>gameData</code> field. */
    public static final String GAME_DATA = "gameData";

    /** The field name of the <code>avrgService</code> field. */
    public static final String AVRG_SERVICE = "avrgService";

    /** The field name of the <code>contentService</code> field. */
    public static final String CONTENT_SERVICE = "contentService";

    /** The field name of the <code>prizeService</code> field. */
    public static final String PRIZE_SERVICE = "prizeService";

    /** The field name of the <code>messageService</code> field. */
    public static final String MESSAGE_SERVICE = "messageService";

    /** The field name of the <code>propertiesService</code> field. */
    public static final String PROPERTIES_SERVICE = "propertiesService";
    // AUTO-GENERATED: FIELDS END

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public DSet<PlayerLocation> playerLocs = DSet.newDSet();

    /** Information on the parties presently in this room. */
    public DSet<PartySummary> parties = DSet.newDSet();

    /** Current party leaders. */
    public DSet<PartyLeader> partyLeaders = DSet.newDSet();

    /** The various game data available to this game. */
    public GameData[] gameData;

    /** Used to communicate with the AVRGameManager. */
    public AVRGameMarshaller avrgService;

    /** The service interface for handling game content. */
    public ContentMarshaller contentService;

    /** The service interface for awarding prizes and trophies. */
    public PrizeMarshaller prizeService;

    /** Used to send messages. */
    public WhirledGameMessageMarshaller messageService;

    /** Used to set game properties. */
    public PropertySpaceMarshaller propertiesService;

    // from PropertySpaceObject
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    // from PropertySpaceObject
    public Set<String> getDirtyProps ()
    {
        return _dirty;
    }

    // AUTO-GENERATED: METHODS START
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
    public void removeFromPlayerLocs (Comparable<?> key)
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
        DSet<PlayerLocation> clone = (value == null) ? null : value.typedClone();
        this.playerLocs = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToParties (PartySummary elem)
    {
        requestEntryAdd(PARTIES, parties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>parties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromParties (Comparable<?> key)
    {
        requestEntryRemove(PARTIES, parties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateParties (PartySummary elem)
    {
        requestEntryUpdate(PARTIES, parties, elem);
    }

    /**
     * Requests that the <code>parties</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setParties (DSet<PartySummary> value)
    {
        requestAttributeChange(PARTIES, value, this.parties);
        DSet<PartySummary> clone = (value == null) ? null : value.typedClone();
        this.parties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPartyLeaders (PartyLeader elem)
    {
        requestEntryAdd(PARTY_LEADERS, partyLeaders, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>partyLeaders</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPartyLeaders (Comparable<?> key)
    {
        requestEntryRemove(PARTY_LEADERS, partyLeaders, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePartyLeaders (PartyLeader elem)
    {
        requestEntryUpdate(PARTY_LEADERS, partyLeaders, elem);
    }

    /**
     * Requests that the <code>partyLeaders</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPartyLeaders (DSet<PartyLeader> value)
    {
        requestAttributeChange(PARTY_LEADERS, value, this.partyLeaders);
        DSet<PartyLeader> clone = (value == null) ? null : value.typedClone();
        this.partyLeaders = clone;
    }

    /**
     * Requests that the <code>gameData</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameData (GameData[] value)
    {
        GameData[] ovalue = this.gameData;
        requestAttributeChange(
            GAME_DATA, value, ovalue);
        this.gameData = (value == null) ? null : value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>gameData</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setGameDataAt (GameData value, int index)
    {
        GameData ovalue = this.gameData[index];
        requestElementUpdate(
            GAME_DATA, index, value, ovalue);
        this.gameData[index] = value;
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

    /**
     * Requests that the <code>contentService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setContentService (ContentMarshaller value)
    {
        ContentMarshaller ovalue = this.contentService;
        requestAttributeChange(
            CONTENT_SERVICE, value, ovalue);
        this.contentService = value;
    }

    /**
     * Requests that the <code>prizeService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPrizeService (PrizeMarshaller value)
    {
        PrizeMarshaller ovalue = this.prizeService;
        requestAttributeChange(
            PRIZE_SERVICE, value, ovalue);
        this.prizeService = value;
    }

    /**
     * Requests that the <code>messageService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMessageService (WhirledGameMessageMarshaller value)
    {
        WhirledGameMessageMarshaller ovalue = this.messageService;
        requestAttributeChange(
            MESSAGE_SERVICE, value, ovalue);
        this.messageService = value;
    }

    /**
     * Requests that the <code>propertiesService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPropertiesService (PropertySpaceMarshaller value)
    {
        PropertySpaceMarshaller ovalue = this.propertiesService;
        requestAttributeChange(
            PROPERTIES_SERVICE, value, ovalue);
        this.propertiesService = value;
    }
    // AUTO-GENERATED: METHODS END

    // from PartyPlaceObject
    public DSet<PartySummary> getParties ()
    {
        return parties;
    }
    
    // from PartyPlaceObject
    public DSet<OccupantInfo> getOccupants ()
    {
        return occupantInfo;
    }
    
    // from PartyPlaceObject
    public DSet<PartyLeader> getPartyLeaders ()
    {
        return partyLeaders;
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        PropertySpaceHelper.writeProperties(this, out);
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        PropertySpaceHelper.readProperties(this, ins);
    }

    /**
     * The current state of game data.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();
}
