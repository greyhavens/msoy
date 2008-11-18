//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

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
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public RoomMarshaller roomService;

    /** Contains the memories for all entities in this room. */
    public DSet<EntityMemoryEntry> memories = new DSet<EntityMemoryEntry>();

    /** Contains mappings for all controlled entities in this room. */
    public DSet<EntityControl> controllers = new DSet<EntityControl>();

    /** The property spaces associated with this room. */
    public DSet<RoomPropertiesEntry> propertySpaces = DSet.newDSet();

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
    // AUTO-GENERATED: METHODS END
}
