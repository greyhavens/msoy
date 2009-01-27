//
// $Id$

package com.threerings.msoy.room.data;

import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.util.ArrayUtil;

import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.party.data.PartySummary;

import static com.threerings.msoy.Log.log;

/**
 * Room stuff.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static final String ROOM_SERVICE = "roomService";

    /** The field name of the <code>memories</code> field. */
    public static final String MEMORIES = "memories";

    /** The field name of the <code>controllers</code> field. */
    public static final String CONTROLLERS = "controllers";

    /** The field name of the <code>propertySpaces</code> field. */
    public static final String PROPERTY_SPACES = "propertySpaces";

    /** The field name of the <code>parties</code> field. */
    public static final String PARTIES = "parties";
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public RoomMarshaller roomService;

    /** Contains the memories for all entities in this room. */
    public DSet<EntityMemoryEntry> memories = DSet.newDSet();

    /** Contains mappings for all controlled entities in this room. */
    public DSet<EntityControl> controllers = DSet.newDSet();

    /** The property spaces associated with this room. */
    public DSet<RoomPropertiesEntry> propertySpaces = DSet.newDSet();

    /** Information on the parties presently in this room. */
    public DSet<PartySummary> parties = DSet.newDSet();

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>roomService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
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
    public void addToMemories (EntityMemoryEntry elem)
    {
        requestEntryAdd(MEMORIES, memories, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memories</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemories (Comparable<?> key)
    {
        requestEntryRemove(MEMORIES, memories, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memories</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemories (EntityMemoryEntry elem)
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
    public void setMemories (DSet<EntityMemoryEntry> value)
    {
        requestAttributeChange(MEMORIES, value, this.memories);
        DSet<EntityMemoryEntry> clone = (value == null) ? null : value.typedClone();
        this.memories = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToControllers (EntityControl elem)
    {
        requestEntryAdd(CONTROLLERS, controllers, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>controllers</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromControllers (Comparable<?> key)
    {
        requestEntryRemove(CONTROLLERS, controllers, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>controllers</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
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
    public void setControllers (DSet<EntityControl> value)
    {
        requestAttributeChange(CONTROLLERS, value, this.controllers);
        DSet<EntityControl> clone = (value == null) ? null : value.typedClone();
        this.controllers = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>propertySpaces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPropertySpaces (RoomPropertiesEntry elem)
    {
        requestEntryAdd(PROPERTY_SPACES, propertySpaces, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>propertySpaces</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPropertySpaces (Comparable<?> key)
    {
        requestEntryRemove(PROPERTY_SPACES, propertySpaces, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>propertySpaces</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
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
    public void setPropertySpaces (DSet<RoomPropertiesEntry> value)
    {
        requestAttributeChange(PROPERTY_SPACES, value, this.propertySpaces);
        DSet<RoomPropertiesEntry> clone = (value == null) ? null : value.typedClone();
        this.propertySpaces = clone;
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
    // AUTO-GENERATED: METHODS END

    /**
     * Put all the specified memories into this room.
     * If there are duplicate entries, a warning is logged, with any log args you specify
     * added to the end of the logging.
     */
    public void putMemories (Iterable<EntityMemoryEntry> mems, Object... logArgs)
    {
        startTransaction();
        try {
            for (EntityMemoryEntry mem : mems) {
                if (memories.contains(mem)) {
                    log.warning("WTF? Room already contains memory entry", ArrayUtil.concatenate(
                        new Object[] {"room", getOid(), "memory", mem}, logArgs));
                } else {
                    addToMemories(mem);
                }
            }
        } finally {
            commitTransaction();
        }
    }

    /**
     * Extract memories from the room that match the specified item, return a List of the
     * memories extracted, or null if none.
     */
    public List<EntityMemoryEntry> takeMemories (ItemIdent ident)
    {
        List<EntityMemoryEntry> list = null;
        for (EntityMemoryEntry entry : memories) {
            if (entry.item.equals(ident)) {
                if (list == null) {
                    list = Lists.newArrayList();
                }
                list.add(entry);
            }
        }

        if (list != null) {
            startTransaction();
            try {
                for (EntityMemoryEntry entry : list) {
                    removeFromMemories(entry.getRemoveKey());
                }
            } finally {
                commitTransaction();
            }
        }
        return list;
    }
}
