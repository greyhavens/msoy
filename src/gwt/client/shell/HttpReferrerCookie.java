//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

/**
 * Wrapper for a cookie that stores HTTP "Referer" tags on the very first visit to
 * a Whirled page, and gets used later on to auto-populate the ReferralInfo struct.
 * 
 * This class contains browser-side functionality; for server-side code see
 * {@link com.threerings.msoy.web.server.HttpReferrerCookie}. 
 */
public class HttpReferrerCookie
{
    /**
     * Is referrer information already stored?
     */
    public static boolean exists ()
    {
        return (CookieUtil.get(REFERRAL_FIELD) != null);
    }

    /**
     * Retrieves saved HTTP referrer. Returns null if one has not been saved.
     */
    public static String get ()
    {
        String ref = CookieUtil.get(REFERRAL_FIELD);
        CShell.log("Loaded referrer: " + ref);
        return ref;
    }

    /**
     * Marks HTTP referrer as "disabled". This will prevent the server from trying
     * to overwrite it on future page views.
     */
    public static void disable (String referrer)
    {
        CookieUtil.set("/", -1, REFERRAL_FIELD, REFERRER_DISABLED_VALUE);
        CShell.log("Referrer disabled.");
    }

    private static final String REFERRAL_FIELD = "ref";
    
    /** 
     * Some value that's not null (so the existence check passes), but
     * also does not contain a meaningful HTTP Reference string. 
     */
    private static final String REFERRER_DISABLED_VALUE = "";
}

