//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.threerings.msoy.swiftly.client.SwiftlyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link SwiftlyService}.
 */
public interface SwiftlyProvider extends InvocationProvider
{
    /**
     * Handles a {@link SwiftlyService#enterProject} request.
     */
    public void enterProject (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
