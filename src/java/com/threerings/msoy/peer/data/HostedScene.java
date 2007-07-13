//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a scene being hosted by this peer.
 */
public class HostedScene extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique identifier for the scene being hosted. */
    public Integer sceneId;

    /** The name of the scene. */
    public String name;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return sceneId;
    }

    /** Used when unserializing. */
    public HostedScene ()
    {
    }

    /**
     * Creates a hosted scene record.
     */
    public HostedScene (int sceneId, String name)
    {
        this.sceneId = sceneId;
        this.name = name;
    }
}
