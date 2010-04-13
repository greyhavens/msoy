//
// $Id$

package com.threerings.msoy.game.server;

import javax.annotation.Generated;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link WorldGameService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from WorldGameService.java.")
public interface WorldGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link WorldGameService#getTablesWaiting} request.
     */
    void getTablesWaiting (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link WorldGameService#inviteFriends} request.
     */
    void inviteFriends (ClientObject caller, int arg1, int[] arg2);

    /**
     * Handles a {@link WorldGameService#locateGame} request.
     */
    void locateGame (ClientObject caller, int arg1, WorldGameService.LocationListener arg2)
        throws InvocationException;
}
