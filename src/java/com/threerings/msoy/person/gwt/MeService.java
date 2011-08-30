//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.gwt.util.ExpanderResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.Contest;

/**
 * Provides information for the Me page.
 */
@RemoteServiceRelativePath(MeService.REL_PATH)
public interface MeService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/mesvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + MeService.ENTRY_POINT;

    /**
     * Loads the data for the MyWhirled view for the calling user.
     */
    MyWhirledData getMyWhirled ()
        throws ServiceException;

    ExpanderResult<Activity> loadStream (long beforeTime, int count)
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

    /**
     * Deletes the given medal from the given member, off of their passport page.
     */
    void deleteEarnedMedal (int memberId, int medalId)
        throws ServiceException;

    /**
     * Select an award to show on the player's profile.
     */
    void selectProfileAward (AwardType type, int awardId)
        throws ServiceException;
}
