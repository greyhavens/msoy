//
// $Id$

package client.people;

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
    public static String getLandingURL (Pages page, boolean friend, Object... args)
    {
        return friend ? page.makeFriendURL(CShell.creds.getMemberId(), args) :
            page.makeAffiliateURL(CShell.creds.getMemberId(), args);
    }
}
