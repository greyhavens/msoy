//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.MediaItem;

/**
 * Contains information about a piece of media in the catalog.
 */
public class MediaData extends SimpleStreamableObject
{
    /** The global id for the media, or -1 to indicate newstyle. */
    public int id;

    /** Newstyle. TODO. */
    public String hash;

    public byte mimeType;

    // more to come

    /**
     * Create a media descriptor from the specified item.
     */
    public static MediaData fromItem (Item item)
    {
        MediaData data = new MediaData(-1);
        if (item instanceof MediaItem) {
            MediaItem mitem = (MediaItem) item;
            data.hash = mitem.mediaHash;
            data.mimeType = mitem.mimeType;

        } else {
            // other kinds of items should have default representations
            // of some special media already in the system...
            // TODO
        }

        return data;
    }

    public static MediaData fromDBString (String encoded)
    {
        if (encoded == null) {
            return null;
        }
        try {
            int colon = encoded.indexOf(':');
            if (colon == -1) {
                return new MediaData(Integer.parseInt(encoded));
            }

            MediaData data = new MediaData(-1);
            data.hash = encoded.substring(0, colon);
            data.mimeType =
                (byte) Integer.parseInt(encoded.substring(colon + 1));
            return data;

        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static String asDBString (MediaData data)
    {
        if (data == null) {
            return null;
        }
        if (data.id == -1) {
            return data.hash + ":" + data.mimeType;
        } else {
            return String.valueOf(data.id);
        }
    }

    /** Suitable for unserialization. */
    public MediaData ()
    {
    }

    public MediaData (int id)
    {
        this.id = id;
    }

    public boolean equals (Object other)
    {
        if (other instanceof MediaData) {
            MediaData that = (MediaData) other;
            return (this.id == that.id) &&
                ObjectUtil.equals(this.hash, that.hash);
        }
        return false;
    }

    // documentation inherited
    public int hashCode ()
    {
        return id;
    }
}
