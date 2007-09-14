//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides game related services.
 */
public interface GameService extends RemoteService
{
    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads the details for the specified game.
     */
    public GameDetail loadGameDetail (WebIdent ident, int gameId)
        throws ServiceException;
}
