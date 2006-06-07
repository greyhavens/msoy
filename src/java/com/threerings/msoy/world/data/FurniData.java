//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.MediaData;

/**
 * Contains information on the location of furniture in a scene.
 */
public class FurniData extends SimpleStreamableObject
{
    /** The id of this piece of furni. */
    public int id;

    /** Info about the media that represents this piece of furni. */
    public MediaData media;

    /** The location in the scene. */
    public MsoyLocation loc;

    // documentation inherited
    public boolean equals (Object other)
    {
        return (other instanceof FurniData) &&
            ((FurniData) other).id == this.id;
    }

    // documentation inherited
    public int hashCode ()
    {
        return id;
    }
}
