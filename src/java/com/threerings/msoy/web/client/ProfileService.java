//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines profile-related services available to the GWT/AJAX web client.
 */
public interface ProfileService extends RemoteService
{
    /**
     * Requests that this user's profile be updated.
     *
     * @param creds an authentication cookie identifying the requesting user.
     */
    public void updateProfile (WebCreds creds, Profile profile);
}
