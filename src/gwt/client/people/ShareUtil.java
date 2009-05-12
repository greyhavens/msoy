//
// $Id$

package client.people;

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
        return page.makeAffiliateURL(CShell.creds.getMemberId(), Args.compose(args));
    }
}
