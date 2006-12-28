//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link ProfileService}.
 */
public interface ProfileServiceAsync
{
    /**
     * The asynchronous version of {@link ProfileService#updateProfile}.
     */
    public void updateProfile (WebCreds creds, Profile profile, AsyncCallback callback);
}
