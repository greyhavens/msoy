//
// $Id$

package com.threerings.msoy.avrg.server;

import javax.annotation.Generated;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.avrg.client.AVRGameAgentService;

/**
 * Defines the server-side of the {@link AVRGameAgentService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from AVRGameAgentService.java.")
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
