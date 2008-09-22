//
// $Id$

package client.shell;

import client.util.StringUtil;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.web.data.TrackingCookieUtil;

/**
 * Wrapper that stores and loads up tracking information: parameters from the
 * external referral, and the unique tracking number.
 */
public class TrackingCookie
{
    /**
     * Does the client cookie already contain referral information?
     */
    public static boolean exists ()
    {
        return (CookieUtil.get(AFFILIATE_ID) != null && CookieUtil.get(VECTOR_ID) != null
             && CookieUtil.get(CREATIVE_ID) != null && CookieUtil.get(TRACKER_ID) != null);
    }

    /**
     * Retrieves referral information. Returns null if one has not been set.
     */
    public static ReferralInfo get ()
    {
        if (!exists()) {
            ReferralInfo ref = new ReferralInfo("", "", "", ReferralInfo.makeRandomTracker());
            CShell.log("Could not locate referralInfo (cookies disabled?), loaded blank info.");
            return ref;
        }

        ReferralInfo ref = ReferralInfo.makeInstance(
            loadCookie(AFFILIATE_ID), loadCookie(VECTOR_ID),
            loadCookie(CREATIVE_ID), loadCookie(TRACKER_ID));

        CShell.log("Loaded referral info: " + ref);
        return ref;
    }

    /**
     * Saves referral information in the cookie.
     *
     * This function will only overwrite old data if the /overwrite/ flag is set to true.
     * Referral infos should only be saved if they don't already exist, or if there's
     * an authoritative version coming from the server.
     */
    public static void save (ReferralInfo referral, boolean overwrite)
    {
        if (exists() && !overwrite) {
            return; // we're not overwriting
        }

        saveCookie(AFFILIATE_ID, referral.affiliate);
        saveCookie(VECTOR_ID, referral.vector);
        saveCookie(CREATIVE_ID, referral.creative);
        saveCookie(TRACKER_ID, referral.tracker);

        CShell.log("Saved referral info: " + referral);
    }

    /**
     * Completely clears the browser cookie. Used when a registered player is logging off.
     */
    public static void clear ()
    {
        String[] ids = { AFFILIATE_ID, VECTOR_ID, CREATIVE_ID, TRACKER_ID };
        for (String id : ids) {
            CookieUtil.clear("/", id);
        }

        CShell.log("Cleared referral info.");
    }

    /**
     * Flattens a ReferralInfo object into a Java object.
     */
    public static native Object getAsObject () /*-{
        var referral = @client.shell.TrackingCookie::get()();
        var result = new Object();
        if (referral != null) {
            result.affiliate = referral.@com.threerings.msoy.data.all.ReferralInfo::affiliate;
            result.vector = referral.@com.threerings.msoy.data.all.ReferralInfo::vector;
            result.creative = referral.@com.threerings.msoy.data.all.ReferralInfo::creative;
            result.tracker = referral.@com.threerings.msoy.data.all.ReferralInfo::tracker;
        }
        return result;
    }-*/;

    /**
     * Saves a ReferralInfo object from a Java object, overwriting the old one if desired.
     */
    public static native void saveAsObject (Object obj, boolean overwrite) /*-{
        var ref = @com.threerings.msoy.data.all.ReferralInfo::makeInstance(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(
            obj.affiliate, obj.vector, obj.creative, obj.tracker);
        @client.shell.TrackingCookie::save(Lcom/threerings/msoy/data/all/ReferralInfo;Z)(ref,overwrite);
    }-*/;

    public static String makeReferralParams (String affiliate, String vector, String creative)
    {
        return "aid_" + (affiliate != null ? affiliate : "") +
            "_" + (vector != null ? vector : "") + "_" + (creative != null ? creative : "");
    }

    /** Obfuscates and saves a cookie. */
    private static void saveCookie (String name, String value)
    {
        // always save the new, obfuscated version
        String encoded = VERSION_2_HEADER + StringUtil.hexlate(TrackingCookieUtil.encode(value));
        CookieUtil.set("/", 365, name, encoded);
    }
    
    /** Loads and de-obfuscates a cookie. */
    private static String loadCookie (String name)
    {
        String value = CookieUtil.get(name);
        if (value == null) {
            return null;
        }
        
        // first, check which version it is
        if (value.startsWith(VERSION_2_HEADER)) {
            // it's the new obfuscated format! let's read it in and decode
            String encoded = value.substring(VERSION_2_HEADER.length());
            return TrackingCookieUtil.decode(StringUtil.unhexlate(encoded));
        }
        
        // otherwise it was a plaintext cookie - just return it untouched
        return value;
    }
    
    private static final String VERSION_2_HEADER = "V2";
    
    private static final String AFFILIATE_ID = "aff";
    private static final String VECTOR_ID = "vec";
    private static final String CREATIVE_ID = "cre";
    private static final String TRACKER_ID = "grp";
}

