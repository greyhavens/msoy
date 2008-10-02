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
    
    /** Occurs when the user attempts to cash out less than the minimum amount of bling. */
    public static String E_BELOW_MINIMUM_BLING = "e.below_minimum_bling";

    /** Occurs when a support member tries to deduct more money than is available. */
    public static String E_MONEY_OVERDRAWN = "e.money_overdrawn";
}
