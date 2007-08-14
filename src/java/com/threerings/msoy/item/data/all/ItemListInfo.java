//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

/**
 * Contains metadata for a list of items.
 */
public class ItemListInfo
    implements Streamable, IsSerializable, DSet.Entry
{
    /** An id constant that means "no list". Not used here. Used in places that use a single
     * int to refer to a list, or no list. */
    public static final int NO_LIST = 0;

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
    public static final byte AUDIO_PLAYLIST = 1;
    public static final byte VIDEO_PLAYLIST = 2;
    public static final byte CATALOG_BUNDLE = 3;

//    public static final byte[] SPECIAL_LIST_TYPES = {
//        CATALOG_BUNDLE, AUDIO_PLAYLIST, VIDEO_PLAYLIST };
//
//    public static final String[] SPECIAL_LIST_NAMES = {
//        "m.ilist_all", "m.ilist_songs", "m.ilist_videos" };

    /** The unique identifier for this list. */
    public int listId;

//    /** The member id of the user that owns this list. */
//    public int memberId;

    /** The type of list that this is. */
    public byte type;

    /** The name of this list. */
    public String name;

    // from DSet.Entry
    public Comparable getKey ()
    {
        // TODO: damn GWT
        return new Integer(listId);
    }

    /**
     * Encode this ItemListInfo into a String, for use as furni actiondata.
     */
    public String encode ()
    {
        return listId + ":" + type + ":" + name;
    }

    public static ItemListInfo decode (String str)
    {
        String[] pieces = str.split(":");
        if (pieces.length == 3) {
            ItemListInfo info = new ItemListInfo();
            try {
                info.listId = Integer.parseInt(pieces[0]);
                info.type = (byte) Integer.parseInt(pieces[1]);
                info.name = pieces[2];
                return info;

            } catch (NumberFormatException nfe) {
                // nothing, fall through
            }
        }

        return null;
    }
}
