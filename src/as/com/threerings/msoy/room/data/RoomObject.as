//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.Iterator;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.item.data.all.Audio;

import com.threerings.msoy.party.data.PartySummary;

import com.threerings.msoy.room.data.EntityMemories;
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

    /** The field name of the <code>parties</code> field. */
    public static const PARTIES :String = "parties";

    /** The field name of the <code>playlist</code> field. */
    public static const PLAYLIST :String = "playlist";

    /** The field name of the <code>currentSongId</code> field. */
    public static const CURRENT_SONG_ID :String = "currentSongId";

    /** The field name of the <code>playCount</code> field. */
    public static const PLAY_COUNT :String = "playCount";
    // AUTO-GENERATED: FIELDS END

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

    /** Contains the memories for all entities in this room. */
    public var memories :DSet;
    EntityMemories; MemoryChangedEvent; // references to force linkage

    /** Contains mappings for all controlled entities in this room. */
    public var controllers :DSet;
    EntityControl; // reference to force linkage

    /** The property spaces associated with this room. */
    public var propertySpaces :DSet;
    RoomPropertiesEntry; // reference to force linkage

    /** Information of the parties presently in this room. */
    public var parties :DSet; /* of */ PartySummary;

    /** The set of songs in the playlist. */
    public var playlist :DSet; /* of */ Audio;

    /** The item id of the current song. */
    public var currentSongId :int;

    /** A monotonically increasing integer used to indicate which song we're playing since
     * the room was first resolved. */
    public var playCount :int;

    /**
     * Finds the info of an occupant who is also a member and has a given member id. Performs the
     * same function as <code>getOccupantInfo(new MemberName("", memberId))</code>, but is more
     * convenient to call and performs better.
     */
    public function getMemberInfo (memberId :int) :MemberInfo
    {
        var itr :Iterator = occupantInfo.iterator();
        while (itr.hasNext()) {
            var minfo :MemberInfo = (itr.next() as MemberInfo);
            if (minfo != null && minfo.getMemberId() == memberId) {
                return minfo;
            }
        }
        return null;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = RoomMarshaller(ins.readObject());
        memories = DSet(ins.readObject());
        controllers = DSet(ins.readObject());
        propertySpaces = DSet(ins.readObject());
        parties = DSet(ins.readObject());
        playlist = DSet(ins.readObject());
        currentSongId = ins.readInt();
        playCount = ins.readInt();
    }
}
}
