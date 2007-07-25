//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.Transient;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.gwt.CatalogListing;

/**
 * Represents a catalog listing of an item.
 */
@Entity
@Table
public abstract class CatalogRecord<T extends ItemRecord> extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #item} field. */
    public static final String ITEM = "item";

    /** The qualified column identifier for the {@link #item} field. */
    public static final ColumnExp ITEM_C =
        new ColumnExp(CatalogRecord.class, ITEM);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(CatalogRecord.class, ITEM_ID);

    /** The column identifier for the {@link #listedDate} field. */
    public static final String LISTED_DATE = "listedDate";

    /** The qualified column identifier for the {@link #listedDate} field. */
    public static final ColumnExp LISTED_DATE_C =
        new ColumnExp(CatalogRecord.class, LISTED_DATE);

    /** The column identifier for the {@link #flowCost} field. */
    public static final String FLOW_COST = "flowCost";

    /** The qualified column identifier for the {@link #flowCost} field. */
    public static final ColumnExp FLOW_COST_C =
        new ColumnExp(CatalogRecord.class, FLOW_COST);

    /** The column identifier for the {@link #goldCost} field. */
    public static final String GOLD_COST = "goldCost";

    /** The qualified column identifier for the {@link #goldCost} field. */
    public static final ColumnExp GOLD_COST_C =
        new ColumnExp(CatalogRecord.class, GOLD_COST);

    /** The column identifier for the {@link #rarity} field. */
    public static final String RARITY = "rarity";

    /** The qualified column identifier for the {@link #rarity} field. */
    public static final ColumnExp RARITY_C =
        new ColumnExp(CatalogRecord.class, RARITY);

    /** The column identifier for the {@link #purchases} field. */
    public static final String PURCHASES = "purchases";

    /** The qualified column identifier for the {@link #purchases} field. */
    public static final ColumnExp PURCHASES_C =
        new ColumnExp(CatalogRecord.class, PURCHASES);

    /** The column identifier for the {@link #returns} field. */
    public static final String RETURNS = "returns";

    /** The qualified column identifier for the {@link #returns} field. */
    public static final ColumnExp RETURNS_C =
        new ColumnExp(CatalogRecord.class, RETURNS);

    /** The column identifier for the {@link #repriceCounter} field. */
    public static final String REPRICE_COUNTER = "repriceCounter";

    /** The qualified column identifier for the {@link #repriceCounter} field. */
    public static final ColumnExp REPRICE_COUNTER_C =
        new ColumnExp(CatalogRecord.class, REPRICE_COUNTER);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 4;

    /** A reference to the listed item. This value is not persisted. */
    @Transient
    public ItemRecord item;

    /** The ID of the listed item. */
    @Id
    public int itemId;

    /** The time this item was listed in the catalog. */
    public Timestamp listedDate;

    /** The amount of flow it costs to purchase a clone of this item. */
    public int flowCost;
    
    /** The amount of gold it costs to purchase a clone of this item. */
    public int goldCost;

    /** The rarity of this item; {@see Item#rarity}. */
    public int rarity;
    
    /** The number of times this item has been purchased. */
    public int purchases;
    
    /** The number of times this item has been returned. */
    public int returns;
    
    /** A somewhat opaque counter representing how badly this record needs to be repriced. */
    public int repriceCounter;

    public CatalogRecord ()
    {
        super();
    }

    protected CatalogRecord (CatalogListing listing)
    {
        super();

        item = ItemRecord.newRecord(listing.item);
        listedDate = new Timestamp(listing.listedDate.getTime());
    }

    public CatalogListing toListing ()
    {
        CatalogListing listing = new CatalogListing();
        listing.item = item.toItem();
        listing.listedDate = new Date(listedDate.getTime());
        // the name part of the MemberName is filled in by ItemManager
        listing.creator = new MemberName(null, item.creatorId);
        listing.flowCost = flowCost;
        listing.goldCost = goldCost;
        listing.rarity = rarity;
        return listing;
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
