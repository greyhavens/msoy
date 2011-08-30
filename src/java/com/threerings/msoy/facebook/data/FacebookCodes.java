//
// $Id$

package com.threerings.msoy.facebook.data;

import com.threerings.presents.data.AuthCodes;

import com.threerings.msoy.data.all.GwtAuthCodes;

/**
 * Codes returned by the facebook servlets.
 */
@com.threerings.util.ActionScript(omit=true)
public interface FacebookCodes extends AuthCodes, GwtAuthCodes
{
    /** Error code when the facebook session is not available. */
    public static final String NO_SESSION = "m.no_session";
}
