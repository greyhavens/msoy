//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.presents.net.Credentials;
import com.threerings.util.Name;

/**
 * Extends the basic credentials to provide window-specific fields.
 * TODO: use something more than the user name to authenticate
 */
public class WindowCredentials extends Credentials
{
    /** Prepended to the bureau id to form a username */
    public static final String PREFIX = "@@bureauwindow:";

    /** Appended to the bureau id to form a username */
    public static final String SUFFIX = "@@";

    /**
     * Test if a given name object matches the name that we generate.
     */
    public static boolean isWindow (Name name)
    {
        String normal = name.getNormal();
        return normal.startsWith(PREFIX) && normal.endsWith(SUFFIX);
    }

    /**
     * Creates an empty credentials for streaming. Should not be used directly.
     */
    public WindowCredentials ()
    {
    }

    /**
     * Creates new credentials for a specific bureau.
     */
    public WindowCredentials (String bureauId, String token)
    {
        super(new Name(PREFIX + bureauId + SUFFIX));
        _token = token;
    }

    public String getToken ()
    {
        return _token;
    }

    @Override // inherit documentation
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append("token=").append(_token);
    }

    protected String _token;
}
