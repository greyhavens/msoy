//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Provides the asynchronous version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The async version of {@link ProfileService#findProfiles}.
     */
    void findProfiles (String search, AsyncCallback<List<MemberCard>> callback);

    /**
     * The async version of {@link ProfileService#loadProfile}.
     */
    void loadProfile (int memberId, AsyncCallback<ProfileService.ProfileResult> callback);

    /**
     * The async version of {@link ProfileService#updateProfile}.
     */
    void updateProfile (int memberId, String displayName, boolean greeter, Profile profile, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#updateInterests}.
     */
    void updateInterests (int memberId, List<Interest> interests, AsyncCallback<Void> callback);

    /**
     * The async version of {@link ProfileService#sendRetentionEmail}.
     */
    void sendRetentionEmail (int profileMemberId, AsyncCallback<Void> callback);
}
