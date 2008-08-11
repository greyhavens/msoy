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

    /** The field name of the <code>effects</code> field. */
    public static final String EFFECTS = "effects";

    /** The field name of the <code>propertySpaces</code> field. */
    public static final String PROPERTY_SPACES = "propertySpaces";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to have occupants load, but not play,
     * the specified music.
     * Format: [ url ].  */
    public static final String LOAD_MUSIC = "loadMusic";

    /** A corresponding message sent by each client when they've got the music
     * completely loaded. No other status is needed.
     * Format: [ url ]. */
    public static final String MUSIC_LOADED = "musicLoaded";

    /** The message sent by the server to kick-off music playing. The music
     * should be played once and then disposed-of. No action
     * should be taken if the music was not loaded.
     * Format: [ url ], or no-args to stop music. */
    public static final String PLAY_MUSIC = "playMusic";

    /** A message sent by each client to indicate that the music has
     * finished playing.
     * Format: [ url ]. */
    public static final String MUSIC_ENDED = "musicEnded";

    /** A message sent by the server when an effect should be added to
     * the specified player's sprite.
     * Format: [ oid, EffectData ]. */
    public static final String ADD_EFFECT = "addEffect";

    /** Our room service marshaller. */
    public RoomMarshaller roomService;

    /** Contains the memories for all entities in this room. */
    public DSet<EntityMemoryEntry> memories = new DSet<EntityMemoryEntry>();

    /** Contains mappings for all controlled entities in this room. */
    public DSet<EntityControl> controllers = new DSet<EntityControl>();

    /** Contains the currently displayed "effects" (temporary furniture..). */
    public DSet<EffectData> effects = new DSet<EffectData>();

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
     * <code>effects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToEffects (EffectData elem)
    {
        requestEntryAdd(EFFECTS, effects, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>effects</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromEffects (Comparable<?> key)
    {
        requestEntryRemove(EFFECTS, effects, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>effects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateEffects (EffectData elem)
    {
        requestEntryUpdate(EFFECTS, effects, elem);
    }

    /**
     * Requests that the <code>effects</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setEffects (DSet<EffectData> value)
    {
        requestAttributeChange(EFFECTS, value, this.effects);
        DSet<EffectData> clone = (value == null) ? null : value.typedClone();
        this.effects = clone;
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
