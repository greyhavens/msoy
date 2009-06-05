//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Contains methods to get URLs for various points in the billing system.
 */
public class BillingUtil
{
    /**
     * Displays the sepecified billing page.
     */
    public static void popBillingPage (String url)
    {
        if (CShell.isGuest()) {
            MsoyUI.infoAction(_msgs.gobuyMustLogon(), _msgs.gobuyLogon(),
                              Link.createHandler(Pages.ACCOUNT, "logon"));

        } else if (CShell.isPermaguest()) {
            MsoyUI.infoAction(_msgs.gobuyMustRegister(), _msgs.gobuyRegister(),
                              Link.createHandler(Pages.ACCOUNT, "create"));

        } else if (MemberMailUtil.isPlaceholderAddress(CShell.creds.accountName)) {
            MsoyUI.infoAction(_msgs.gobuyMustConfigure(), _msgs.gobuyConfigure(),
                              Link.createHandler(Pages.ACCOUNT, "config"));

        } else {
            Window.open(BASE + url + ((url.indexOf("?") == -1) ? "?" : "&") +
                        "initUsername=" + URL.encodeComponent(CShell.creds.accountName),
                        "_blank",
                        // For those silly browsers that open this in a new window instead of a new
                        // tab, enable all the chrome options on the new window.
                        "resizable=1,menubar=1,toolbar=1,location=1,status=1,scrollbars=1");
        }
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
            URL.encodeComponent(permaName == null ? accountName : permaName);
    }

    protected static String capPath (String path)
    {
        return path.endsWith("/") ? path : (path + "/");
    }

    protected static final String BASE = capPath(DeploymentConfig.billingURL);
    protected static final BillingMessages _msgs = GWT.create(BillingMessages.class);
}
