//
// $Id$

package com.threerings.msoy.item.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the item services.
 */
public interface ItemCodes extends InvocationCodes
{
    /** An error code returned by the item services. */
    public static final String E_NO_SUCH_ITEM = "e.no_such_item";

    /** An error code returned by the item services. */
    public static final String E_ITEM_IN_USE = "e.item_in_use";

    /** An error code returned by the item services. */
    public static final String E_ITEM_LISTED = "e.item_listed";
    
    /** An error code for any operation that needs to debit a member's flow. */
    public static final String INSUFFICIENT_FLOW = "m.insufficient_flow";

    /** An error code for any operation that needs to debit a member's gold. */
    public static final String INSUFFICIENT_GOLD = "m.insufficient_gold";
}
