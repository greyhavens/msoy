//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * Contains credentials obtained from Facebook Connect.
 */
public class FacebookCreds extends ExternalCreds
{
    /** The user's Facebook UID. From the APIKEY_user cookie. */
    public String uid;

    /** The user's session key. From the APIKEY_session_key cookie. */
    public String sessionKey;

    /** The user's session secret. From the APIKEY_ss cookie. */
    public String ss;

    /** The ctime when the session expires or 0 if it doesn't. From the APIKEY_expires cookie. */
    public int expires;

    /** The signature computed for these credentials. From the APIKEY cookie. */
    public String sig;

    /** The site that these credentials come from. Normally just FB_GAMES. */
    public ExternalSiteId site;

    /**
     * Returns true if all of our fields have some non-blank value. Note that we can only validate
     * the *actual* validity of the values on the server where we can recompute and verify {@link
     * #sig}.
     */
    public boolean haveAllFields ()
    {
        return (uid.length() > 0) && (sessionKey.length() > 0) && (ss.length() > 0) &&
            (sig.length() > 0);
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
        return sessionKey;
    }

    @Override // from Object
    public String toString ()
    {
        return getSite() + "[uid=" + uid + ", key=" + sessionKey + ", ss=" + ss +
            ", exp=" + expires + ", sig=" + sig + "]";
    }
}
