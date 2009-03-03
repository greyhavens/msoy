//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#loadProfile}.
     */
    void loadProfile (int memberId, AsyncCallback<ProfileService.ProfileResult> callback);

    /**
     * The asynchronous version of {@link ProfileService#updateProfile}.
     */
    void updateProfile (
        String displayName, boolean greeter, Profile profile, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ProfileService#updateInterests}.
     */
    void updateInterests (List<Interest> interests, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ProfileService#findProfiles}.
     */
    void findProfiles (String search, AsyncCallback<List<MemberCard>> callback);

    /**
     * The asynchronous version of {@link ProfileService#loadSelfFeed}.
     */
    void loadSelfFeed (int profileMemberId, int cutoffDays,
                       AsyncCallback<List<FeedMessage>> callback);
}
