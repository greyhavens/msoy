//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Contains metadata for a list of items.
 */
public class ItemListInfo
    implements Streamable, DSet_Entry
{
    /** An id constant that means "no list". Not used here. Used in places that use a single
     * int to refer to a list, or no list. */
    public static const NO_LIST :int = 0;

//    /** An id constant that refers to the list of all catalog items that this user created. */
//    public static final int LISTED_ALL = -1;
//
//    /** An id constant that refers to all audio items owned by the user. */
//    public static final int OWNED_AUDIO = -2;
//
//    /** An id constant that refers to all audio items owned by the user. */
//    public static final int OWNED_VIDEO = -3;
//
//    public static final int SPECIAL_LIST_COUNT = 3;

    /** The type constants. */
    public static const AUDIO_PLAYLIST :int = 1;

    public static const VIDEO_PLAYLIST :int = 2;

    public static const CATALOG_BUNDLE :int = 3;

//    public static final byte[] SPECIAL_LIST_TYPES = {
//        CATALOG_BUNDLE, AUDIO_PLAYLIST, VIDEO_PLAYLIST };
//
//    public static final String[] SPECIAL_LIST_NAMES = {
//        "m.ilist_all", "m.ilist_songs", "m.ilist_videos" };

    /** The unique identifier for this list. */
    public var listId :int;

//    /** The member id of the user that owns this list. */
//    public int memberId;

    /** The type of list that this is. */
    public var type :int;

    /** The name of this list. */
    public var name :String;

    public static function decode (arg1 :String) :ItemListInfo
    {
        throw new Error("Not implemented in ActionScript.");
    }

    public function ItemListInfo ()
    {
        // used for unserialization
    }

    /**
     * Encode this ItemListInfo into a String, for use as furni actiondata.
     */
    public function encode () :String
    {
        throw new Error("Not implemented in ActionScript.");
    }

    // from DSet.Entry
    public function getKey () :Object
    {
        return listId;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        listId = ins.readInt();
        type = ins.readByte();
        name = (ins.readField(String) as String);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(listId);
        out.writeByte(type);
        out.writeField(name);
    }
}
}
