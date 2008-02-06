//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.WhatIsWhirledData;
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
     * Loads the data for the MyWhirled view for the calling user.
     */
    public MyWhirledData getMyWhirled (WebIdent ident)
        throws ServiceException;

    /**
     * Loads the data for the Whirledwide page.
     */
    public WhirledwideData getWhirledwide ()
        throws ServiceException;

    /**
     * Loads the data for the WhatIsWhirled page.
     */
    public WhatIsWhirledData getWhatIsWhirled ()
        throws ServiceException;

    /**
     * Updates the Whirledwide news HTML. Caller must be an admin.
     */
    public void updateWhirledNews (WebIdent ident, String newsHtml)
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
