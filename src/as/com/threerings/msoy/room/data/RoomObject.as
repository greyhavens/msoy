//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.RoomPropertiesEntry;

/**
 * Contains the distributed state of a virtual world room.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static const ROOM_SERVICE :String = "roomService";

    /** The field name of the <code>memories</code> field. */
    public static const MEMORIES :String = "memories";

    /** The field name of the <code>controllers</code> field. */
    public static const CONTROLLERS :String = "controllers";

    /** The field name of the <code>propertySpaces</code> field. */
    public static const PROPERTY_SPACES :String = "propertySpaces";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to have occupants load, but not play,
     * the specified music.
     * Format: [ url | AudioItem ].  */
    public static const LOAD_MUSIC :String = "loadMusic";

    /** A corresponding message sent by each client when they've got the music
     * completely loaded. No other status is needed.
     * Format: [ url | MediaDesc ]. */
    public static const MUSIC_LOADED :String = "musicLoaded";

    /** The message sent by the server to kick-off music playing. The music
     * should be played once and then disposed-of. No action
     * should be taken if the music was not loaded. 
     * Format: [ url | Audio item ]. */
    public static const PLAY_MUSIC :String = "playMusic";

    /** A message sent by each client to indicate that the music has
     * finished playing.
     * Format: [ url | MediaDesc ]. */
    public static const MUSIC_ENDED :String = "musicEnded";

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

    /** Contains the memories for all entities in this room. */
    public var memories :DSet = new DSet();
    EntityMemoryEntry; // reference to force linkage

    /** Contains mappings for all controlled entities in this room. */
    public var controllers :DSet = new DSet();
    EntityControl; // reference to force linkage

    /** The property spaces associated with this room. */
    public var propertySpaces :DSet = new DSet();
    RoomPropertiesEntry; // reference to force linkage
    
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = RoomMarshaller(ins.readObject());
        memories = DSet(ins.readObject());
        controllers = DSet(ins.readObject());
        propertySpaces = DSet(ins.readObject());
    }
}
}
