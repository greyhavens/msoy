//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;

/**
 * Contains information about a piece of media in the catalog.
 */
public class MediaData extends SimpleStreamableObject
{
    /** The global id for the media. */
    public int id;

    // more to come

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
        return (other instanceof MediaData) && ((MediaData) other).id == id;
    }

    // documentation inherited
    public int hashCode ()
    {
        return id;
    }
}
