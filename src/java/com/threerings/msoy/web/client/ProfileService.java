//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /**
     * Requests that the header information be updated in a user's profile.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public void updateProfileHeader (WebCreds creds, String displayName,
                                     String homePageURL, String headline);

    /**
     * Requests that the header information be updated in a user's profile.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public void updateProfileDetails (WebCreds creds, boolean isMale,
                                      long birthday, String location);
}
