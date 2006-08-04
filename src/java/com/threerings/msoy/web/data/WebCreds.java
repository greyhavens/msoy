//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information used to keep track of who we are in our GWT web
 * application.
 */
public class WebCreds implements IsSerializable
{
    /** Our member id. */
    public int memberId;

    /** Our session token. */
    public String token;

    /**
     * Extracts our credential from a cookie string.
     *
     * @return null if the cookie was null or unparseable, a set of credentials
     * if the cookie was valid.
     */
    public static WebCreds fromCookie (String cookie)
    {
        int semidx;
        if (cookie == null || (semidx = cookie.indexOf(";")) == -1) {
            return null;
        }
        try {
            WebCreds creds = new WebCreds();
            creds.memberId = Integer.parseInt(cookie.substring(0, semidx));
            creds.token = cookie.substring(semidx+1);
            return creds;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts these credentials to a string that can be stored in a cookie.
     */
    public String toCookie ()
    {
        return memberId + ";" + token;
    }

    /**
     * Generates a string representaton of this instance.
     */
    public String toString ()
    {
        return "[id=" + memberId + ", token=" + token + "]";
    }
}
