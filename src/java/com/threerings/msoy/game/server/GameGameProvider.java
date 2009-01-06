//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.GameGameService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link GameGameService}.
 */
public interface GameGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link GameGameService#getTrophies} request.
     */
    void getTrophies (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
