//
// $Id$

package com.threerings.msoy.world.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides information related to the world.
 */
public interface WorldService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/worldsvc";

    /**
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    String serializePopularPlaces (WebIdent ident, int n)
        throws ServiceException;

    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads information on a particular room.
     */
    RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException;
}
