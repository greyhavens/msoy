//
// $Id$

package com.threerings.msoy.facebook.data;

import com.threerings.msoy.data.all.GwtAuthCodes;
import com.threerings.presents.data.AuthCodes;

/**
 * Codes returned by the facebook servlets.
 */
public interface FacebookCodes extends AuthCodes, GwtAuthCodes
{
    /** Error code when the facebook session is not available. */
    public static final String NO_SESSION = "m.no_session";
}
