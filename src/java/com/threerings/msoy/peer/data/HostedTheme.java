//
// $Id: HostedPlace.java 10239 2008-07-30 14:40:34Z mdb $

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a hosted theme on a particular server.
 */
public class HostedTheme extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The unique identifier for the place being hosted. */
    public Integer themeId;

    /** The popularity of this theme, see {@link ThemeRecord#popularity}. */
    public Integer popularity;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return themeId;
    }

    /** Used when unserializing. */
    public HostedTheme ()
    {
    }

    /**
     * Creates a hosted place record.
     */
    public HostedTheme (int themeId, int popularity)
    {
        this.themeId = themeId;
        this.popularity = popularity;
    }
}
