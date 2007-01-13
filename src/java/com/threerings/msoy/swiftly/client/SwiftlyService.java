//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides services to the Swiftly editor client.
 */
public interface SwiftlyService extends InvocationService
{
    /**
     * Resolves and reports the room oid for the specified project.
     */
    public void enterProject (Client client, int projectId, ResultListener listener);
}
