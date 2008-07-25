//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.data.all.ReferralInfo;

/**
 * Wrapper that stores and loads up tracking information: parameters from the
 * external referral, and the unique tracking number.
 */
public class TrackingCookie
{
    /**
     * Does the client cookie already contain referral information?
     */
    public static boolean contains ()
    {
        return (CookieUtil.get(AFFILIATE_ID) != null);
    }

    /**
     * Retrieves referral information. Returns null if one has not been set.
     */
    public static ReferralInfo get ()
    {
        if (! contains()) {
            ReferralInfo ref = new ReferralInfo("", "", "", ReferralInfo.makeRandomTracker());
            CShell.log("Could not locate referralInfo (cookies disabled?), loaded blank info.");
            return ref;
        }

        ReferralInfo ref = ReferralInfo.makeInstance(
            CookieUtil.get(AFFILIATE_ID), CookieUtil.get(VECTOR_ID),
            CookieUtil.get(CREATIVE_ID), CookieUtil.get(TRACKER_ID));

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
        if (contains() && !overwrite) {
            return; // we're not overwriting
        }

        CookieUtil.set("/", 365, AFFILIATE_ID, referral.affiliate);
        CookieUtil.set("/", 365, VECTOR_ID, referral.vector);
        CookieUtil.set("/", 365, CREATIVE_ID, referral.creative);
        CookieUtil.set("/", 365, TRACKER_ID, referral.tracker);

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
      var ref =
        @com.threerings.msoy.data.all.ReferralInfo::makeInstance(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(obj.affiliate, obj.vector, obj.creative, obj.tracker);

      @client.shell.TrackingCookie::save(Lcom/threerings/msoy/data/all/ReferralInfo;Z)(ref,overwrite);
    }-*/;

    public static String makeReferralParams (String affiliate, String vector, String creative) 
    {
        return "aid_" + (affiliate != null ? affiliate : "") + 
            "_" + (vector != null ? vector : "") + "_" + (creative != null ? creative : "");
    }
    
    private static final String AFFILIATE_ID = "aff";
    private static final String VECTOR_ID = "vec";
    private static final String CREATIVE_ID = "cre";
    private static final String TRACKER_ID = "grp";
}

