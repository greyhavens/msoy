//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRepository;

/**
 * Handles the collection of Swiftly project information
 */
public class SwiftlyManager
{
    /**
     * Configures us with our repository.
     */
    public void init (SwiftlyProjectRepository sprepo)
    {
        _sprepo = sprepo;
    }

    /** Handles persistent stuff. */
    protected SwiftlyProjectRepository _sprepo;
}
