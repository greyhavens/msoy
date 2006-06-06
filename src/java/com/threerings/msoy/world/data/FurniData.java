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
    public MediaData mediaData;

    /** The location in the scene. */
    public MsoyLocation loc;
}
