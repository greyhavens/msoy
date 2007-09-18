//
// $Id$

package com.threerings.msoy.item.data.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a catalog listing of an item.
 */
public class CatalogListing
    implements Streamable, IsSerializable
{
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_RATING = 1;
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_LIST_DATE = 2;
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_SATISFACTION = 3;
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_PRICE_ASC = 4;
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_PRICE_DESC = 5;
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static final byte SORT_BY_PURCHASES = 6;

    /** A constant that encodes the rarity of a listed item. */
    public static final int RARITY_PLENTIFUL = 1;
    /** A constant that encodes the rarity of a listed item. */
    public static final int RARITY_COMMON = 2;
    /** A constant that encodes the rarity of a listed item. */
    public static final int RARITY_NORMAL = 3;
    /** A constant that encodes the rarity of a listed item. */
    public static final int RARITY_UNCOMMON = 4;
    /** A constant that encodes the rarity of a listed item. */
    public static final int RARITY_RARE = 5;

    /** The unique id for this listing. */
    public int catalogId;

    /** The item being listed. */
    public Item item;

    /** The date on which the item was listed. */
    public Date listedDate;

    /** The creator of the item. */
    public MemberName creator;

    /** The amount of flow it costs to purchase this item, if it's listed, else zero. */
    public int flowCost;

    /** The amount of gold it costs to purchase this item, if it's listed, else zero. */
    public int goldCost;

    /** The rarity of this item, if it's listed, else zero. */
    public int rarity;

    /** The number of purchases of this item. */
    public int purchases;

    /** The number of returns of this item. */
    public int returns;
}
