//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link WorldGameService}.
 */
public interface WorldGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link WorldGameService#joinWorldGame} request.
     */
    public void joinWorldGame (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
