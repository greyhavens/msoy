//
// $Id$

package com.threerings.msoy.swiftly.util;

import com.threerings.crowd.util.CrowdContext;
import com.threerings.util.MessageManager;

import com.threerings.msoy.swiftly.client.SwiftlyEditor;

/**
 * Provides necessary services, and juicy goodness.
 */
public interface SwiftlyContext extends CrowdContext
{
    /**
     * Provides access to translation message bundles.
     */
    public MessageManager getMessageManager ();

    /** Translates the specified message using the specified message bundle. */
    public String xlate (String bundle, String message);
}
