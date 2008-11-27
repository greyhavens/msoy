//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides information for the Me page.
 */
public interface MeService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/mesvc";

    /**
     * Loads the data for the MyWhirled view for the calling user.
     */
    MyWhirledData getMyWhirled ()
        throws ServiceException;

    /**
     * Loads the data for one category of the feed for the me page. If fullsize is true include up
     * to 50 items from the last week, otherwise 3.
     */
    FeedCategory loadFeedCategory (int category, boolean fullSize)
        throws ServiceException;

    /**
     * Loads the badges relevant to this player.  If the memberId is the same as the caller,
     * the nextBadges field will be filled, null otherwise.
     */
    PassportData loadBadges (int memberId)
        throws ServiceException;

    /**
     * Loads all available badges. For testing only.
     */
    List<Badge> loadAllBadges()
        throws ServiceException;

    /**
     * Loads all active contests.
     */
    List<Contest> loadContests ()
        throws ServiceException;
}
