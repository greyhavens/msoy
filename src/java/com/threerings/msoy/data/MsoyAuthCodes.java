//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.data.AuthCodes;

import com.threerings.msoy.data.all.GwtAuthCodes;

/**
 * Additional auth codes for the MetaSOY server.
 */
public interface MsoyAuthCodes extends AuthCodes, GwtAuthCodes
{
    /** An error code we use on msoy when the username or password are invalid. We 
     * do not tell people if an email address is valid, that's a security risk. */
    public static final String INVALID_LOGON = "m.invalid_logon";

    /** An error code sent when the server is experiencing a high load. The client should tell the
     * user to try again later, or automatically retry after a certain amount of time. */
    public static final String UNDER_LOAD = "m.under_load";
}
