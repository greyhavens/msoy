//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoyGameService}.
 */
public interface MsoyGameProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyGameService#inviteFriends} request.
     */
    public void inviteFriends (ClientObject caller, int arg1, int[] arg2);

    /**
     * Handles a {@link MsoyGameService#locateGame} request.
     */
    public void locateGame (ClientObject caller, int arg1, MsoyGameService.LocationListener arg2)
        throws InvocationException;
}
