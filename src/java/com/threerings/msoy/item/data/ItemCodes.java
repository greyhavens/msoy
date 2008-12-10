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

    /** An error code returned by the item services. */
    public static final String E_ITEM_NOT_LISTED = "e.item_not_listed";

    /** An error code returned by the item services. */
    public static final String E_HIT_SALES_LIMIT = "e.hit_sales_limit";

    /** An error code returned by the item services. */
    public static final String E_CANT_SELF_CROSSBUY = "e.cant_self_crossbuy";

    /** An error code for listing sub items whose super item is not listed. */
    public static final String E_SUPER_ITEM_NOT_LISTED = "e.list_super_item";

    /** An error code for delisting super items that have listed sub items. */
    public static final String E_NO_DELIST_LISTED_SUBITEM_HAVER = "e.no_delist_listed_subitem_haver";

    /** An error code indicating charities cannot list an item for bars. */
    public static final String E_CHARITIES_CANNOT_LIST_FOR_BARS = "e.charities_cannot_list_for_bars";
}
