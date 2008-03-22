//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.net.AuthResponseData;

/**
 * Extends the normal auth response data with MSOY-specific bits.
 */
public class MsoyAuthResponseData extends AuthResponseData
{
    /** The session token assigned to this user, or null. */
    public String sessionToken;

    /** A machine identifier to be assigned to this machine, or null. */
    public String ident;

    /** A possible warning message to the user, or null. */
    public String warning;
}
