//
// $Id$

package com.threerings.msoy.server;

import com.threerings.crowd.server.PlaceManager;

/**
 * Does something extraordinary.
 */
public class SimpleChatManager extends PlaceManager
{
    // documentation inherited
    protected long idleUnloadPeriod ()
    {
        return 0L; // don't ever unload this place
    }
}
