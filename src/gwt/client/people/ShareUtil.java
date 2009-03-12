//
// $Id$

package client.people;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;

/**
 * Share-related utility methods.
 */
public class ShareUtil
{
    /**
     * Returns an affiliated link (for the current member) to the specified page.
     */
    public static String getAffiliateLandingURL (Pages page, Object... args)
    {
        String path = DeploymentConfig.serverURL + "welcome/" + CShell.creds.getMemberId();
        if (page != Pages.LANDING) {
            path += "/" + Pages.makeToken(page, Args.compose(args));
        }
        return path;
    }
}
