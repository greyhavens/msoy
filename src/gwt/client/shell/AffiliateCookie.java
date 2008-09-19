//
// $Id$

package client.shell;

import com.threerings.gwt.util.CookieUtil;

/**
 * Client-side of {@link com.threerings.msoy.web.server.AffiliateCookie}.
 */
public class AffiliateCookie
{
    /** {@link com.threerings.msoy.web.server.AffiliateCookie#NAME} */
    public static final String NAME = "baff";

    /**
     * Clear the affiliate cookie. It's set on the server!
     */
    public static void clear ()
    {
        CookieUtil.clear("/", NAME);
    }
}
