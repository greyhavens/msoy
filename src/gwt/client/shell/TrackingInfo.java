//
// $Id$

package client.shell;

import java.lang.System;
import com.google.gwt.user.client.Random;

import com.threerings.gwt.util.CookieUtil;

/**
 * Wrapper that stores and loads up tracking information: parameters from the
 * external referral, and the unique tracking number.
 */
public class TrackingInfo
{
    public static final long NOT_TRACKED = 0;
    
    public enum Type {
        AFFILIATE_ID("affiliateId"),
        VECTOR_ID("vectorId"),
        CREATIVE_ID("creativeId");

        public String getKey () {
            return key;
        }

        private Type (String key) {
            this.key = key;
        }

        private final String key;
    }        
    
    /**
     * Does the client cookie already contain referral information?
     */
    public static boolean containsReferral ()
    {
        return (CookieUtil.get(Type.AFFILIATE_ID.getKey()) != null);
    }

    /**
     * Does the client cookie already contain a tracking number?
     */
    public static boolean containsTracker ()
    {
        return (CookieUtil.get(TRACKING_NUMBER_ID) != null);
    }

    /**
     * Retrieves referral information of desired type. Returns null if one has not been set.
     */
    public static String getReferral (Type type)
    {
        String result = CookieUtil.get(type.getKey());
        CShell.log("Loaded referral info [type=" + type.getKey() + ", val=" + result + "].");
        return result;
    }

    /**
     * Retrieves the tracking number. Returns NOT_TRACKED if one has not been set.
     */
    public static long getTracker ()
    {
        String result = CookieUtil.get(TRACKING_NUMBER_ID);
        if (result == null) {
            return NOT_TRACKED;
        }

        try {
            return Long.parseLong(result);
            
        } catch (Exception e) {
            // if anything happens, bail - someone else will need to regenerate this
            CShell.log("Invalid group id [id=" + result + "].");
            return NOT_TRACKED;
        }
    }

    /**
     * Saves referral information in the cookie.
     *
     * This function will only overwrite old data if the /overwrite/ flag is set to true.
     * Referral infos should only be saved if they don't already exist, or if it's
     * the authoritative version coming from the server. 
     */
    public static void saveReferral (
        String affiliate, String vector, String creative, boolean overwrite)
    {
        if (containsReferral() && !overwrite) {
            return; // we're not overwriting
        }

        CookieUtil.set("/", 365, Type.AFFILIATE_ID.getKey(), affiliate);
        CookieUtil.set("/", 365, Type.VECTOR_ID.getKey(), vector);
        CookieUtil.set("/", 365, Type.CREATIVE_ID.getKey(), creative);
        CShell.log("Saved affiliate id [affiliate=" + affiliate + ", vector=" +
            vector + ", creative=" + creative + "].");
    }

    /**
     * Creates and saves a new tracking number, as needed. Does not overwrite an existing one.
     */
    public static void addTracker ()
    {
        if (containsTracker()) {
            return; // we're not overwriting
        }

        // otherwise, take current system time in milliseconds, shift it left by four *decimal*
        // digits (so that it's easy to see what's going on without hex formatting), and fill in
        // the lower four digits with a random value. This gives us a resolution of 10000 unique
        // tracking numbers per millisecond.
        long now = System.currentTimeMillis() * 10000;
        long rand = Random.nextInt() % 10000;
        saveTracker(now + rand, false);
    }
    
    /**
     * Sets the tracking number, used to assign test groups. 
     *
     * This function will only overwrite old data if the /overwrite/ flag is set to true.
     * Referral infos should only be saved if they don't already exist, or if it's
     * the authoritative version coming from the server. 
     */    
    public static void saveTracker (long tracker, boolean overwrite)
    {
        if (containsTracker() && !overwrite) {
            return; // nothing to do
        }

        CookieUtil.set("/", 365, TRACKING_NUMBER_ID, Long.toString(tracker));
        CShell.log("Saved group id [group=" + tracker + "].");
    }        

    private static final String TRACKING_NUMBER_ID = "groupId";
}
    
