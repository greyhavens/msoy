//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.person.gwt.MeService.AwardType;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.web.gwt.Contest;

/**
 * The asynchronous (client-side) version of {@link MeService}.
 */
public interface MeServiceAsync
{
    /**
     * The asynchronous version of {@link MeService#getMyWhirled}.
     */
    void getMyWhirled (AsyncCallback<MyWhirledData> callback);

    /**
     * The asynchronous version of {@link MeService#loadFeedCategory}.
     */
    void loadFeedCategory (int category, boolean fullSize, AsyncCallback<FeedCategory> callback);

    /**
     * The asynchronous version of {@link MeService#loadBadges}.
     */
    void loadBadges (int memberId, AsyncCallback<PassportData> callback);

    /**
     * Load all available badges.  For testing.
     */
    void loadAllBadges (AsyncCallback<List<Badge>> callback);

    /**
     * The asynchronous version of {@link MeService#loadContests}.
     */
    void loadContests (AsyncCallback<List<Contest>> callback);

    /**
     * The asynchronous version of {@link MeService#deleteEarnedMedal}.
     */
    void deleteEarnedMedal (int memberId, int medalId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link MeService#selectProfileAward}.
     */
    void selectProfileAward (AwardType type, int awardId, AsyncCallback<Void> callback);
}
