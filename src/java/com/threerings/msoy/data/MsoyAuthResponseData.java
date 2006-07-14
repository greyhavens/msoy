//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.net.AuthResponseData;

/**
 * Extends the normal auth response data with MSOY-specific bits.
 */
public class MsoyAuthResponseData extends AuthResponseData
{
    /** A machine identifier to be assigned to this machine. */
    public String ident;
}
