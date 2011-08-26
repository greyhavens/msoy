//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.presents.data.AuthCodes;
import com.threerings.presents.data.InvocationCodes;

/**
 * Error codes returned by the world provider implementation.
 */
@com.threerings.util.ActionScript(omit=true)
public interface WorldCodes extends AuthCodes, InvocationCodes
{
    /** Raised when a group home was requested but the group could not be found. */
    public static final String NO_SUCH_GROUP = "m.no_such_group";
}
