//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.WhirledwideData;

/**
 * Provides information related to the world (and the whirled).
 */
public interface WorldService extends RemoteService
{
    /**
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    public String serializePopularPlaces (WebIdent ident, int n)
        throws ServiceException;

    /**
     * Get data for the MyWhirled view for the current user.
     */
    public MyWhirledData getMyWhirled (WebIdent ident)
        throws ServiceException;

    /**
     * Get the data for the Whirledwide view for the current user.
     */
    public WhirledwideData getWhirledwide ()
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     *
     * @gwt.typeArgs <com.threerings.msoy.person.data.FeedMessage>
     */
    public List loadFeed (WebIdent ident, int cutoffDays)
        throws ServiceException;

    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException;
}
