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
    /** Special list type used to indicate a member's favorite items. */
    public static final byte FAVORITES = -1;
    
    /** Generic item list type */
    public static final byte GENERIC = 0;
    
    /** The type constants. */
    public static final byte AUDIO_PLAYLIST = 1;
    public static final byte VIDEO_PLAYLIST = 2;
    public static final byte CATALOG_BUNDLE = 3;

    /**
     * A name for the special favorites list.
     * TODO how will the i18n work here?
     */
    public static final String FAVORITES_NAME = "m.favorites";
    
    /** The unique identifier for this list. */
    public int listId;

    /** The member id of the user that owns this list. */
    public int memberId;

    /** The type of list that this is. */
    public byte type = GENERIC;

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
        return listId + ":" + memberId + ":" + type + ":" + name;
    }

    public static ItemListInfo decode (String str)
    {
        String[] pieces = str.split(":");
        if (pieces.length == 4) {
            ItemListInfo info = new ItemListInfo();
            try {
                info.listId = Integer.parseInt(pieces[0]);
                info.memberId = (byte) Integer.parseInt(pieces[1]);
                info.type = (byte) Integer.parseInt(pieces[2]);
                info.name = pieces[3];
                return info;

            } catch (NumberFormatException nfe) {
                // nothing, fall through
            }
        }

        return null;
    }
}
