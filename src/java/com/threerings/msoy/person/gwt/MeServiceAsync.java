//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.ExpanderResult;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.Contest;

/**
 * Provides the asynchronous version of {@link MeService}.
 */
public interface MeServiceAsync
{
    /**
     * The async version of {@link MeService#loadContests}.
     */
    void loadContests (AsyncCallback<List<Contest>> callback);

    /**
     * The async version of {@link MeService#getMyWhirled}.
     */
    void getMyWhirled (AsyncCallback<MyWhirledData> callback);

    /**
     * The async version of {@link MeService#loadFeedCategory}.
     */
    void loadFeedCategory (FeedMessageType.Category category, boolean fullSize, AsyncCallback<MyWhirledData.FeedCategory> callback);

    /**
     * The async version of {@link MeService#loadStream}.
     */
    void loadStream (long beforeTime, int count, AsyncCallback<ExpanderResult<Activity>> callback);

    /**
     * The async version of {@link MeService#loadBadges}.
     */
    void loadBadges (int memberId, AsyncCallback<PassportData> callback);

    /**
     * The async version of {@link MeService#loadAllBadges}.
     */
    void loadAllBadges (AsyncCallback<List<Badge>> callback);

    /**
     * The async version of {@link MeService#deleteEarnedMedal}.
     */
    void deleteEarnedMedal (int memberId, int medalId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link MeService#selectProfileAward}.
     */
    void selectProfileAward (Award.AwardType type, int awardId, AsyncCallback<Void> callback);
}
