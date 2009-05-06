//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;

import com.google.inject.Singleton;

import com.samskivert.util.StringUtil;

import com.google.code.facebookapi.FacebookJaxbRestClient;

import com.threerings.msoy.web.gwt.FacebookCreds;

/**
 * Centralizes some Facebook API bits.
 */
@Singleton
public class FacebookLogic
{
    /**
     * Returns a Facebook client not bound to any particular user's session.
     */
    public FacebookJaxbRestClient getFacebookClient ()
    {
        return new FacebookJaxbRestClient(SERVER_URL, requireAPIKey(), requireSecret(),
                                          null, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Returns a Facebook client bound to the supplied user's session.
     */
    public FacebookJaxbRestClient getFacebookClient (FacebookCreds creds)
    {
        return new FacebookJaxbRestClient(SERVER_URL, requireAPIKey(), requireSecret(),
                                          creds.sessionKey, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    protected String requireAPIKey ()
    {
        String apiKey = ServerConfig.config.getValue("facebook.api_key", "");
        if (StringUtil.isBlank(apiKey)) {
            throw new IllegalStateException("Missing facebook.api_key server configuration.");
        }
        return apiKey;
    }

    protected String requireSecret ()
    {
        String secret = ServerConfig.config.getValue("facebook.secret", "");
        if (StringUtil.isBlank(secret)) {
            throw new IllegalStateException("Missing facebook.secret server configuration.");
        }
        return secret;
    }

    protected static final int CONNECT_TIMEOUT = 15*1000; // in millis
    protected static final int READ_TIMEOUT = 15*1000; // in millis

    protected static final URL SERVER_URL;
    static {
        try {
            SERVER_URL = new URL("http://api.facebook.com/restserver.php");
        } catch (Exception e) {
            throw new RuntimeException(e); // MalformedURLException should be unchecked, sigh
        }
    }
}
