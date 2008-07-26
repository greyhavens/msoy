//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.bureau.client.ThaneWorldService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ThaneWorldService}.
 */
public interface ThaneWorldProvider extends InvocationProvider
{
    /**
     * Handles a {@link ThaneWorldService#locateRoom} request.
     */
    void locateRoom (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
