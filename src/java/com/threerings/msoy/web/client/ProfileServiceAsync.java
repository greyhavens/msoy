//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#updateProfileHeader}.
     */
    public void updateProfileHeader (WebCreds creds, String displayName,
                                     String homePageURL, String headline,
                                     AsyncCallback callback);

    /**
     * The asynchronous version of {@link ProfileService#updateProfileDetails}.
     */
    public void updateProfileDetails (WebCreds creds, boolean isMale,
                                      long birthday, String location,
                                      AsyncCallback callback);
}
