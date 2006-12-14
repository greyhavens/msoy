//
// $Id$

package com.threerings.msoy.item.web;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.web.data.MemberName;

/**
 * Represents a catalog listing of an item.
 */
public class CatalogListing
    implements Streamable, IsSerializable
{
    public static final byte SORT_BY_RATING = 1;
    public static final byte SORT_BY_LIST_DATE = 2;
    public static final byte SORT_BY_SATISFACTION = 3;
    public static final byte SORT_BY_PRICE = 4;
    
    /** The item being listed. */
    public Item item;

    /** The date on which the item was listed. */
    public Date listedDate;

    /** The creator of the item. */
    public MemberName creator;

    /** The current price of the item. */
    public int price;
}
