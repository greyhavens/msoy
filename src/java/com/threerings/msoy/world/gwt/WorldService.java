//
// $Id$

package com.threerings.msoy.world.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.LaunchConfig;
import com.threerings.msoy.group.gwt.MyWhirledData;

import com.threerings.msoy.web.data.RoomInfo;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.LandingData;

import com.threerings.msoy.person.gwt.FeedMessage;

/**
 * Provides information related to the world (and the whirled).
 */
public interface WorldService extends RemoteService
{
    /** Contains information about one of our rooms. */
    public static class Room implements IsSerializable
    {
        /** The room's scene id. */
        public int sceneId;

        /** The room's name. */
        public String name;

        /** The room's decor thumbnail image. */
        public MediaDesc decor;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/worldsvc";

    /**
     * Loads the data for the landing page.
     */
    public LandingData getLandingData ()
        throws ServiceException;

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
     * Updates the Whirled news HTML. Caller must be an admin.
     */
    public void updateWhirledNews (WebIdent ident, String newsHtml)
        throws ServiceException;

    /**
     * Loads the list of rooms owned by the calling user.
     */
    public List<Room> loadMyRooms (WebIdent ident)
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     */
    public List<FeedMessage> loadFeed (WebIdent ident, int cutoffDays)
        throws ServiceException;

    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    public LaunchConfig loadLaunchConfig (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads information on a particular room.
     */
    public RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException;
}
