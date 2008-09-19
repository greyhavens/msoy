//
// $Id$

package client.shell;

import client.util.StringUtil;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.msoy.data.all.ReferralInfo;

/**
 * Client-side of {@link com.threerings.msoy.web.server.AffiliateCookie}.
 */
public class AffiliateCookie
{
    /** {@link com.threerings.msoy.web.server.AffiliateCookie#NAME} */
    public static final String NAME = "baff";

    public static void clear ()
    {
        CookieUtil.clear("/", NAME);
    }
}
