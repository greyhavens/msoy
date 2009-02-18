//
// $Id$

package client.util;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.WebCreds;

import client.shell.CShell;

/**
 * Contains methods to get URLs for various points in the billing system.
 */
public class BillingUtil
{
    /**
     * Pops up a window to the billing system for buying bars. This and {@link #onBuyBars} are the
     * <em>only</em> way that the billing system should be linked to.
     */
    public static void goBuyBars ()
    {
        String args = ((CShell.creds == null) ? "" : ("?initUsername=" + CShell.creds.accountName));
        Window.open(LANDING + args, "_blank",
                    // For those silly browsers that open this in a new window instead of a new
                    // tab, enable all the chrome options on the new window.
                    "resizable=1,menubar=1,toolbar=1,location=1,status=1,scrollbars=1");
    }

    /**
     * When clicked, popup up a window to billing to buy bars.
     */
    public static ClickListener onBuyBars ()
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                goBuyBars();
            }
        };
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
