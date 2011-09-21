//
// $Id$

package com.threerings.msoy.game.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.game.client.GameGameService;

/**
 * Defines the server-side of the {@link GameGameService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from GameGameService.java.")
public interface GameGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link GameGameService#complainPlayer} request.
     */
    void complainPlayer (ClientObject caller, int arg1, String arg2);

    /**
     * Handles a {@link GameGameService#getTrophies} request.
     */
    void getTrophies (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link GameGameService#removeDevelopmentTrophies} request.
     */
    void removeDevelopmentTrophies (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
