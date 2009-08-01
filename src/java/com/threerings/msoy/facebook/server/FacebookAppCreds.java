//
// $Id$

package com.threerings.msoy.facebook.server;

import com.threerings.msoy.web.gwt.ExternalAuther;
import com.threerings.msoy.web.gwt.ExternalCreds;

/**
 * Credentials for a user authenticating from an iframed Facebook app. These are never sent
 * over the wire.
 */
public class FacebookAppCreds extends ExternalCreds
{
    /** The Facebook user id of the user in question. */
    public String uid;

    /** The API key of the app via which the user is authenticating. */
    public String apiKey;

    /** The app secret of the app via which the user is authenticating. */
    public String appSecret;

    /** The session key of the viewing user (may be null). */
    public String sessionKey;

    @Override // from ExternalCreds
    public ExternalAuther getAuthSource () {
        return ExternalAuther.FACEBOOK;
    }

    @Override // from ExternalCreds
    public String getUserId () {
        return uid;
    }

    @Override // from ExternalCreds
    public String getPlaceholderAddress () {
        return uid + "@facebook.com";
    }

    @Override // from ExternalCreds
    public String getSessionKey () {
        return sessionKey;
    }
}
