//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * Contains credentials obtained from Facebook Connect.
 */
public class FacebookCreds extends ExternalCreds
{
    /** The user's Facebook UID. */
    public String uid;

    /** A token that can be used to make requests to FB. */
    public String accessToken;

    /** Used to validate these credentials. */
    public String signedRequest;

    /** The site that these credentials come from. Normally just FB_GAMES. */
    public ExternalSiteId site;

    /**
     * Returns true if all of our fields have some non-blank value. Note that we can only validate
     * the *actual* validity of the values on the server where we can recompute and verify {@link
     * #sig}.
     */
    public boolean haveAllFields ()
    {
        return (uid.length() > 0) && (accessToken.length() > 0) && (signedRequest.length() > 0);
    }

    @Override // from ExternalCreds
    public ExternalSiteId getSite ()
    {
        return site;
    }

    @Override // from ExternalCreds
    public String getUserId ()
    {
        return uid;
    }

    @Override // from ExternalCreds
    public String getPlaceholderAddress ()
    {
        return uid + "@facebook.com";
    }

    @Override // from ExternalCreds
    public String getSessionKey ()
    {
        return accessToken;
    }

    @Override // from Object
    public String toString ()
    {
        return getSite() + "[uid=" + uid + ", accessToken=" + accessToken +
            ", signedRequest=" + signedRequest + "]";
    }
}
