//
// $Id$

package com.threerings.msoy.room.data;

import javax.annotation.Generated;
import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Room stuff.
 */
@com.threerings.util.ActionScript(omit=true)
public class RoomObject extends SpotSceneObject
    implements PartyPlaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>name</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String NAME = "name";

    /** The field name of the <code>owner</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String OWNER = "owner";

    /** The field name of the <code>accessControl</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ACCESS_CONTROL = "accessControl";

    /** The field name of the <code>roomService</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String ROOM_SERVICE = "roomService";

    /** The field name of the <code>memories</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String MEMORIES = "memories";

    /** The field name of the <code>controllers</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String CONTROLLERS = "controllers";

    /** The field name of the <code>propertySpaces</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PROPERTY_SPACES = "propertySpaces";

    /** The field name of the <code>parties</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTIES = "parties";

    /** The field name of the <code>partyLeaders</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PARTY_LEADERS = "partyLeaders";

    /** The field name of the <code>playlist</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PLAYLIST = "playlist";

    /** The field name of the <code>currentSongId</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String CURRENT_SONG_ID = "currentSongId";

    /** The field name of the <code>playCount</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String PLAY_COUNT = "playCount";
    // AUTO-GENERATED: FIELDS END

    /** The name of this room. */
    public String name;

    /** The name of the owner of this room (MemberName or GroupName). */
    public Name owner;

    /** Access control, as one of the ACCESS constants. Limits who can enter the scene. */
    public byte accessControl;

    /** Our room service marshaller. */
    public RoomMarshaller roomService;

    /** Contains the memories for all entities in this room. */
    public DSet<EntityMemories> memories = DSet.newDSet();

    /** Contains mappings for all controlled entities in this room. */
    public DSet<EntityControl> controllers = DSet.newDSet();

    /** The property spaces associated with this room. */
    public DSet<RoomPropertiesEntry> propertySpaces = DSet.newDSet();

    /** Information on the parties presently in this room. */
    public DSet<PartySummary> parties = DSet.newDSet();

    /** Current party leaders, even if they're not in this room. */
    public DSet<PartyLeader> partyLeaders = DSet.newDSet();

    /** The set of songs in the playlist. */
    public DSet<Audio> playlist = DSet.newDSet();

    /** The item id of the current song. */
    public int currentSongId;

    /** A monotonically increasing integer used to indicate which song we're playing since
      * the room was first resolved. */
    public int playCount;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>name</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setName (String value)
    {
        String ovalue = this.name;
        requestAttributeChange(
            NAME, value, ovalue);
        this.name = value;
    }

    /**
     * Requests that the <code>owner</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setOwner (Name value)
    {
        Name ovalue = this.owner;
        requestAttributeChange(
            OWNER, value, ovalue);
        this.owner = value;
    }

    /**
     * Requests that the <code>accessControl</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setAccessControl (byte value)
    {
        byte ovalue = this.accessControl;
        requestAttributeChange(
            ACCESS_CONTROL, Byte.valueOf(value), Byte.valueOf(ovalue));
        this.accessControl = value;
    }

    /**
     * Requests that the <code>roomService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setRoomService (RoomMarshaller value)
    {
        RoomMarshaller ovalue = this.roomService;
        requestAttributeChange(
            ROOM_SERVICE, value, ovalue);
        this.roomService = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToMemories (EntityMemories elem)
    {
        requestEntryAdd(MEMORIES, memories, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memories</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromMemories (Comparable<?> key)
    {
        requestEntryRemove(MEMORIES, memories, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateMemories (EntityMemories elem)
    {
        requestEntryUpdate(MEMORIES, memories, elem);
    }

    /**
     * Requests that the <code>memories</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setMemories (DSet<EntityMemories> value)
    {
        requestAttributeChange(MEMORIES, value, this.memories);
        DSet<EntityMemories> clone = (value == null) ? null : value.clone();
        this.memories = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToControllers (EntityControl elem)
    {
        requestEntryAdd(CONTROLLERS, controllers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>controllers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromControllers (Comparable<?> key)
    {
        requestEntryRemove(CONTROLLERS, controllers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updateControllers (EntityControl elem)
    {
        requestEntryUpdate(CONTROLLERS, controllers, elem);
    }

    /**
     * Requests that the <code>controllers</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setControllers (DSet<EntityControl> value)
    {
        requestAttributeChange(CONTROLLERS, value, this.controllers);
        DSet<EntityControl> clone = (value == null) ? null : value.clone();
        this.controllers = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>propertySpaces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToPropertySpaces (RoomPropertiesEntry elem)
    {
        requestEntryAdd(PROPERTY_SPACES, propertySpaces, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>propertySpaces</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromPropertySpaces (Comparable<?> key)
    {
        requestEntryRemove(PROPERTY_SPACES, propertySpaces, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>propertySpaces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updatePropertySpaces (RoomPropertiesEntry elem)
    {
        requestEntryUpdate(PROPERTY_SPACES, propertySpaces, elem);
    }

    /**
     * Requests that the <code>propertySpaces</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPropertySpaces (DSet<RoomPropertiesEntry> value)
    {
        requestAttributeChange(PROPERTY_SPACES, value, this.propertySpaces);
        DSet<RoomPropertiesEntry> clone = (value == null) ? null : value.clone();
        this.propertySpaces = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToParties (PartySummary elem)
    {
        requestEntryAdd(PARTIES, parties, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>parties</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromParties (Comparable<?> key)
    {
        requestEntryRemove(PARTIES, parties, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>parties</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setParties (DSet<PartySummary> value)
    {
        requestAttributeChange(PARTIES, value, this.parties);
        DSet<PartySummary> clone = (value == null) ? null : value.clone();
        this.parties = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToPartyLeaders (PartyLeader elem)
    {
        requestEntryAdd(PARTY_LEADERS, partyLeaders, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>partyLeaders</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromPartyLeaders (Comparable<?> key)
    {
        requestEntryRemove(PARTY_LEADERS, partyLeaders, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>partyLeaders</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
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
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPartyLeaders (DSet<PartyLeader> value)
    {
        requestAttributeChange(PARTY_LEADERS, value, this.partyLeaders);
        DSet<PartyLeader> clone = (value == null) ? null : value.clone();
        this.partyLeaders = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>playlist</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void addToPlaylist (Audio elem)
    {
        requestEntryAdd(PLAYLIST, playlist, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>playlist</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void removeFromPlaylist (Comparable<?> key)
    {
        requestEntryRemove(PLAYLIST, playlist, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>playlist</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void updatePlaylist (Audio elem)
    {
        requestEntryUpdate(PLAYLIST, playlist, elem);
    }

    /**
     * Requests that the <code>playlist</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPlaylist (DSet<Audio> value)
    {
        requestAttributeChange(PLAYLIST, value, this.playlist);
        DSet<Audio> clone = (value == null) ? null : value.clone();
        this.playlist = clone;
    }

    /**
     * Requests that the <code>currentSongId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setCurrentSongId (int value)
    {
        int ovalue = this.currentSongId;
        requestAttributeChange(
            CURRENT_SONG_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.currentSongId = value;
    }

    /**
     * Requests that the <code>playCount</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setPlayCount (int value)
    {
        int ovalue = this.playCount;
        requestAttributeChange(
            PLAY_COUNT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.playCount = value;
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
     * Do whatever's necessary to update the specified memory value.
     */
    public void updateMemory (ItemIdent ident, String key, byte[] value)
    {
        if (value == null || memories.containsKey(ident)) {
            // we're removing or already have an entry for this item, let's use our special event.
            // Note that we dispatch the event for the remove *even if there are no memories*,
            // the remove is still valid and the special event will take care of notifying
            // listeners without actually modifying anything.
            MemoryChangedEvent mce = new MemoryChangedEvent(_oid, MEMORIES, ident, key, value);
            // if we're on the authoritative server, update things immediately.
            if (_omgr != null && _omgr.isManager(this)) {
                mce.applyToObject(this);
            }
            postEvent(mce);

        } else {
            // We do not have an entry and we're adding a new value.
            // This form of the constructor marks the memories modified immediately.
            addToMemories(new EntityMemories(ident, key, value));
        }
    }

    /**
     * Put the specified memories into this room.
     */
    public void putMemories (EntityMemories mems)
    {
        if (memories.contains(mems)) {
            log.warning("WTF? Room already contains memory entry",
                "room", getOid(), "entityIdent", mems.getKey(), new Exception());
            updateMemories(mems);
        } else {
            addToMemories(mems);
        }
    }

    /**
     * Extract memories from the room that match the specified item, return the
     * memories extracted, or null if none.
     */
    public EntityMemories takeMemories (ItemIdent ident)
    {
        EntityMemories mems = memories.get(ident);
        if (mems != null) {
            removeFromMemories(ident);
        }
        return mems;
    }
}
