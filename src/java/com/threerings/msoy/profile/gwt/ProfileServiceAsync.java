//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.ExpanderResult;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Provides the asynchronous version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The async version of {@link ProfileService#loadActivity}.
     */
    void loadActivity (int memberId, long beforeTime, int count, AsyncCallback<ExpanderResult<Activity>> callback);

    /**
     * The async version of {@link ProfileService#updateProfile}.
     */
    void updateProfile (int memberId, String displayName, boolean greeter, Profile profile, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#updateInterests}.
     */
    void updateInterests (int memberId, List<Interest> interests, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#findProfiles}.
     */
    void findProfiles (String search, AsyncCallback<List<MemberCard>> callback);

    /**
     * The async version of {@link ProfileService#sendRetentionEmail}.
     */
    void sendRetentionEmail (int profileMemberId, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#poke}.
     */
    void poke (int memberId, AsyncCallback<FeedMessage> callback);

    /**
     * The async version of {@link ProfileService#complainProfile}.
     */
    void complainProfile (int memberId, String description, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#loadProfile}.
     */
    void loadProfile (int memberId, AsyncCallback<ProfileService.ProfileResult> callback);
}
