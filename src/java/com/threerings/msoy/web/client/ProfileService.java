//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /**
     * Loads the profile of the supplied member id.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public Profile loadProfile (String creds, int memberId);

    /**
     * Requests that the header information be updated in a user's profile.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public void updateProfileHeader (String creds, String displayName,
                                     String homePageURL, String headline);

    /**
     * Requests that the header information be updated in a user's profile.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public void updateProfileDetails (String creds, boolean isMale,
                                      long birthday, String location);
}
