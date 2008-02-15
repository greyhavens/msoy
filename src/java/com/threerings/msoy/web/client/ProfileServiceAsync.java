//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#updateProfile}.
     */
    public void updateProfile (WebIdent ident, String displayName, Profile profile,
                               AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#loadProfile}.
     */
    public void loadProfile (WebIdent ident, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#findProfiles}.
     */
    public void findProfiles (WebIdent ident, String search, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#loadFriends}.
     */
    public void loadFriends (WebIdent ident, int memberId, AsyncCallback callback);
}
