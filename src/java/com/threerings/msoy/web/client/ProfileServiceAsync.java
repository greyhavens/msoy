//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#loadProfile}.
     */
    public void loadProfile (String creds, int memberId,
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#updateProfileHeader}.
     */
    public void updateProfileHeader (String creds, String displayName,
                                     String homePageURL, String headline,
                                     AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#updateProfileDetails}.
     */
    public void updateProfileDetails (String creds, boolean isMale,
                                      long birthday, String location,
                                      AsyncCallback callback);
}
