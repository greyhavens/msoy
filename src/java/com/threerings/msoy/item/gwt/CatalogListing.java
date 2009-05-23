//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

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
    public static final int MIN_SALES_TARGET = DeploymentConfig.devDeployment ? 5 : 100;

    /**
     * Escalates the supplied price based on the escalation factor.
     */
    public static final int escalatePrice (int value)
    {
        return Math.round(value + value * ESCALATION_FACTOR);
    }

    /**
     * Gets the minimum cost of a derived item given the currency and cost of the basis.
     */
    public static int getMinimumDerivedCost (Currency currency, int basisCost)
    {
        switch (currency) {
        case COINS:
            return basisCost + 10;
        case BARS:
            return basisCost + 1;
        default:
            throw new IllegalArgumentException("Invalid listing currency " + currency);
        }
    }

    /**
     * Just enough information to link to a derived item.
     */
    public static class DerivedItem
        implements IsSerializable
    {
        public int catalogId;
        public String name;
    }

    /**
     * Just enough information to link to the basis item and show the creator.
     */
    public static class BasisItem
        implements IsSerializable
    {
        public int catalogId;
        public String name;
        public MemberName creator;
    }

    /** The unique id for this listing. */
    public int catalogId;

    /** The details of the listed item (including the item). */
    public ItemDetail detail;

    /** The item id of the original from which this listing was created. */
    public int originalItemId;

    /** The date on which the item was listed. */
    public Date listedDate;

    /** The reserved price of the item. */
    public PriceQuote quote;

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

    /** The catalog id of item that was used to create this item. Set voluntarily. */
    public int basisId;

    /** If requested, set to the item referred to by {@link #basisId}. */
    public BasisItem basis;

    /** The number of item listings that are based on this item. */
    public int derivationCount;

    /** If requested, a sampling of the listings that are based on this one. */
    public DerivedItem[] derivatives;

    /** The amount by which we increase the price at each escalation. */
    protected static final float ESCALATION_FACTOR = 0.25f;
}
