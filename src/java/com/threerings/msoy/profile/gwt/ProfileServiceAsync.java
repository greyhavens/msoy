//
// $Id$

package com.threerings.msoy.profile.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.Interest;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#loadProfile}.
     */
    public void loadProfile (WebIdent ident, int memberId,
                             AsyncCallback<ProfileService.ProfileResult> callback);

    /**
     * The asynchronous version of {@link ProfileService#updateProfile}.
     */
    public void updateProfile (WebIdent ident, String displayName, Profile profile,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ProfileService#updateInterests}.
     */
    void updateInterests (WebIdent ident, List<Interest> interests, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ProfileService#findProfiles}.
     */
    public void findProfiles (WebIdent ident, String search,
                              AsyncCallback<List<MemberCard>> callback);

    /**
     * The asynchronous version of {@link ProfileService#loadSelfFeed}.
     */
    public void loadSelfFeed (int profileMemberId, int cutoffDays,
                              AsyncCallback<List<FeedMessage>> callback);
}
