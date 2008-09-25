//
// $Id$

package com.threerings.msoy.money.data;

import com.threerings.presents.data.InvocationCodes;

public interface MoneyCodes extends InvocationCodes
{
    /** Occurs when the user has insufficient bling for an operation. */
    public static String E_INSUFFICIENT_BLING = "e.insufficient_bling";
    
    /** Occurs when the user attempts to cash out bling when they've already cashed out. */
    public static String E_ALREADY_CASHED_OUT = "e.already_cashed_out";
}
