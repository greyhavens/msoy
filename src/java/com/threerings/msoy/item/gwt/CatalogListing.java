//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Represents a catalog listing of an item.
 */
public class CatalogListing
    implements Streamable, IsSerializable
{
    /** An item that is not actually for sale (used for game awards). */
    public static final int PRICING_HIDDEN = 1;
    /** An item with a manually configured price. */
    public static final int PRICING_MANUAL = 2;
    /** An item whose price should be adjusted as it hits sales targets. */
    public static final int PRICING_ESCALATE = 3;
    /** A limited edition item that should be delisted when it hits its sales target. */
    public static final int PRICING_LIMITED_EDITION = 4;

    /** Enumerates all available pricing strategies. */
    public static final int[] PRICING = {
        PRICING_HIDDEN, PRICING_MANUAL, PRICING_ESCALATE, PRICING_LIMITED_EDITION };

    /** The minimum run for escalating pricing and limited edition. */
    public static final int MIN_SALES_TARGET = 100;

    /**
     * Escalates the supplied price based on the escalation factor.
     */
    public static final int escalatePrice (int value)
    {
        return Math.round(value + value * ESCALATION_FACTOR);
    }

    /** The unique id for this listing. */
    public int catalogId;

    /** The details of the listed item (including the item). */
    public ItemDetail detail;

    /** The item id of the original from which this listing was created. */
    public int originalItemId;

    /** The date on which the item was listed. */
    public Date listedDate;

    /** The currency this item is listed for. */
    public Currency currency;

    /** The price of this item. */
    public int cost;

    /** The pricing setting for this item. */
    public int pricing;

    /** Used by items with {@link #PRICING_ESCALATE} or {@link #PRICING_LIMITED_EDITION}. */
    public int salesTarget;

    /** The number of purchases of this item. */
    public int purchases;

    /** The number of returns of this item. */
    public int returns;

    /** The number of people who consider this item a favorite. */
    public int favoriteCount;

    /** The amount by which we increase the price at each escalation. */
    protected static final float ESCALATION_FACTOR = 0.25f;
}
