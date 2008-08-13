//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides information for the Me page.
 */
public interface MeService extends RemoteService
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
    public static final String ENTRY_POINT = "/mesvc";

    /**
     * Loads the data for the MyWhirled view for the calling user.
     */
    MyWhirledData getMyWhirled ()
        throws ServiceException;

    /**
     * Updates the Whirled news HTML. Caller must be an admin.
     */
    void updateWhirledNews (String newsHtml)
        throws ServiceException;

    /**
     * Loads the list of rooms owned by the calling user.
     */
    List<Room> loadMyRooms ()
        throws ServiceException;

    /**
     * Loads all items in a player's inventory of the specified type and optionally restricted to
     * the specified suite.
     */
    List<FeedMessage> loadFeed (int cutoffDays)
        throws ServiceException;

    /**
     * Loads the badges relevant to this player.
     */
    PassportData loadBadges ()
        throws ServiceException;

    /**
     * Loads all available badges. For testing only.
     */
    List<Badge> loadAllBadges()
        throws ServiceException;
}
