//
// $Id$

package com.threerings.msoy.web.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.Profile;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends RemoteServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public Profile loadProfile (String creds, int memberId)
    {
        return new Profile();
    }

    // from interface ProfileService
    public void updateProfileHeader (
        String creds, String displayName, String homePageURL, String headline)
    {
    }

    // from interface ProfileService
    public void updateProfileDetails (
        String creds, boolean isMale, long birthday, String location)
    {
    }
}
