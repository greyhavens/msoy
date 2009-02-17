//
// $Id$

package client.util;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Contains methods to get URLs for various points in the billing system.
 */
public final class BillingURLs
{
    /**
     * Returns the entry point into the billing system.
     *
     * @param creds If not null, this should contain the account name to be filled in automatically
     * on the login page.
     */
    public static String getEntryPoint (WebCreds creds)
    {
        if (creds != null) {
            return LANDING + "?initUsername=" + creds.accountName;
        }
        return LANDING;
    }

    /**
     * Returns a URL to the page showing transaction history for a particular user.  Must be
     * logged in as admin in billing to see this page.
     *
     * @param accountName Account name of the user (login email address)
     * @param permaName Permaname of the user, if chosen.
     */
    public static String getUserStatusPage (String accountName, String permaName)
    {
        return BASE + "admin/user_status.wm?username=" +
            (permaName == null ? accountName : permaName);
    }

    protected static String capPath (String path)
    {
        return path.endsWith("/") ? path : (path + "/");
    }

    protected static final String BASE = capPath(DeploymentConfig.billingURL);
    protected static final String LANDING = BASE + "whirled.wm";
}
