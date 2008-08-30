//
// $Id$

package com.threerings.msoy.avrg.server;

import com.threerings.msoy.avrg.client.AVRGameAgentService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link AVRGameAgentService}.
 */
public interface AVRGameAgentProvider extends InvocationProvider
{
    /**
     * Handles a {@link AVRGameAgentService#leaveGame} request.
     */
    void leaveGame (ClientObject caller, int arg1);

    /**
     * Handles a {@link AVRGameAgentService#roomSubscriptionComplete} request.
     */
    void roomSubscriptionComplete (ClientObject caller, int arg1);
}
