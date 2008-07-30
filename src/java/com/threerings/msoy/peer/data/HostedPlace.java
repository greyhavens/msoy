//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a hosted scene on a particular server.
 */
public class HostedPlace extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique identifier for the place being hosted. */
    public Integer placeId;

    /** The name of this place. */
    public String name;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return placeId;
    }

    /** Used when unserializing. */
    public HostedPlace ()
    {
    }

    /**
     * Creates a hosted place record.
     */
    public HostedPlace (int placeId, String name)
    {
        this.placeId = placeId;
        this.name = name;
    }
}
