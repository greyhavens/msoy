//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.ShellMessages;
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
            MsoyUI.infoAction(_cmsgs.gobuyMustLogon(), _cmsgs.gobuyLogon(),
                              Link.createHandler(Pages.ACCOUNT, "logon"));

        } else if (CShell.isPermaguest()) {
            MsoyUI.infoAction(_cmsgs.gobuyMustRegister(), _cmsgs.gobuyRegister(),
                              NaviUtil.onMustRegister());

        } else if (MemberMailUtil.isPlaceholderAddress(CShell.creds.accountName)) {
            MsoyUI.infoAction(_cmsgs.gobuyMustConfigure(), _cmsgs.gobuyConfigure(),
                              Link.createHandler(Pages.ACCOUNT, "config"));

        } else {
            Window.open(getAbsoluteBillingURL(url),
                        "_blank",
                        // For those silly browsers that open this in a new window instead of a new
                        // tab, enable all the chrome options on the new window.
                        "resizable=1,menubar=1,toolbar=1,location=1,status=1,scrollbars=1");
        }
    }

    /**
     * Display an appropriate error dialog if the user's account is not ready for billing,
     * otherwise send the user to #billing-args
     */
    public static void openBillingPage (final Object... args)
    {
        if (CShell.isGuest()) {
            MsoyUI.infoAction(_cmsgs.gobuyMustLogon(), _cmsgs.gobuyLogon(), Link.createHandler(
                Pages.ACCOUNT, "logon"));

        } else if (CShell.isPermaguest()) {
            MsoyUI.infoAction(_cmsgs.gobuyMustRegister(), _cmsgs.gobuyRegister(),
                NaviUtil.onMustRegister());

        } else if (MemberMailUtil.isPlaceholderAddress(CShell.creds.accountName)) {
            MsoyUI.infoAction(_cmsgs.gobuyMustConfigure(), _cmsgs.gobuyConfigure(),
                Link.createHandler(Pages.ACCOUNT, "config"));

        } else {
            Link.go(Pages.BILLING, args);
        }
    }

    /**
     * Return true if the user is registered and has an actual email address on file, meaning they
     * are ready to be sent to the billing system.
     */
    public static boolean isBillingReady ()
    {
        return (!CShell.isGuest() && !CShell.isPermaguest()
            && !MemberMailUtil.isPlaceholderAddress(CShell.creds.accountName));
    }

    /**
     * Return a url to the specified page on the billing system, including auth credentials.
     * @param path Relative page to a page on billing eg "paypal/choosecoins.jspx"
     */
    public static String getAbsoluteBillingURL (String path)
    {
        return BASE + path + ((path.indexOf("?") == -1) ? "?" : "&") +
        "initUsername=" + URL.encodeComponent(CShell.creds.accountName);
    }

    /**
     * Returns the billing account status page URL.
     */
    public static String getAccountStatusPage ()
    {
        return BASE + "status.wm?initUsername=" + URL.encodeComponent(CShell.creds.accountName);
    }

    /**
     * Returns a URL to the page showing transaction history for a particular user.  Must be logged
     * in as admin in billing to see this page.
     *
     * @param accountName account name of the user (login email address).
     * @param permaName permaname of the user, if configured.
     */
    public static String getAdminStatusPage (String accountName, String permaName)
    {
        return BASE + "admin/user_status.wm?username=" +
            URL.encodeComponent(permaName == null ? accountName : permaName);
    }

    protected static String capPath (String path)
    {
        return path.endsWith("/") ? path : (path + "/");
    }

    protected static final String BASE = capPath(DeploymentConfig.billingURL);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
