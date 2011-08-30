//
// $Id: HostedPlace.java 10239 2008-07-30 14:40:34Z mdb $

package com.threerings.msoy.peer.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.group.server.persist.ThemeRecord;

/**
 * Represents a hosted theme on a particular server.
 */
@com.threerings.util.ActionScript(omit=true)
public class HostedTheme extends HostedPlace
    implements DSet.Entry
{
    /** The popularity of this theme, see {@link ThemeRecord#popularity}. */
    public Integer popularity;

    /** Used when unserializing. */
    public HostedTheme ()
    {
    }

    /**
     * Creates a hosted place record.
     */
    public HostedTheme (int themeId, String name, int popularity)
    {
        super(themeId, name);
        this.popularity = popularity;
    }
}
