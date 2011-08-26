//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.data.AuthCodes;

import com.threerings.msoy.data.all.GwtAuthCodes;

/**
 * Additional auth codes for the MetaSOY server.
 */
@com.threerings.util.ActionScript(omit=true)
public interface MsoyAuthCodes extends AuthCodes, GwtAuthCodes
{
    /** An error code we use on msoy when the username or password are invalid. We 
     * do not tell people if an email address is valid, that's a security risk. */
    public static final String INVALID_LOGON = "m.invalid_logon";
}
