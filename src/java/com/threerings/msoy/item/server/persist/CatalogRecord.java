//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.Transient;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.gwt.CatalogListing;

/**
 * Represents a catalog listing of an item.
 */
@Entity(indices={
    @Index(name="listedItemIndex", fields={ CatalogRecord.LISTED_ITEM_ID } )
})
public abstract class CatalogRecord<T extends ItemRecord> extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #catalogId} field. */
    public static final String CATALOG_ID = "catalogId";

    /** The column identifier for the {@link #listedItemId} field. */
    public static final String LISTED_ITEM_ID = "listedItemId";

    /** The column identifier for the {@link #originalItemId} field. */
    public static final String ORIGINAL_ITEM_ID = "originalItemId";

    /** The column identifier for the {@link #listedDate} field. */
    public static final String LISTED_DATE = "listedDate";

    /** The column identifier for the {@link #flowCost} field. */
    public static final String FLOW_COST = "flowCost";

    /** The column identifier for the {@link #goldCost} field. */
    public static final String GOLD_COST = "goldCost";

    /** The column identifier for the {@link #rarity} field. */
    public static final String RARITY = "rarity";

    /** The column identifier for the {@link #purchases} field. */
    public static final String PURCHASES = "purchases";

    /** The column identifier for the {@link #returns} field. */
    public static final String RETURNS = "returns";

    /** The column identifier for the {@link #repriceCounter} field. */
    public static final String REPRICE_COUNTER = "repriceCounter";
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 7;

    /** A unique id assigned to this catalog listing. */
    @Id
    @GeneratedValue(generator="catalogId", strategy=GenerationType.TABLE, allocationSize=1)
    public int catalogId;

    /** The id of the listed item. */
    public int listedItemId;

    /** The id of the original item used to create this listing. */
    public int originalItemId;

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

    /** A reference to the listed item. This value is not persisted. */
    @Transient
    public ItemRecord item;

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
        listing.catalogId = catalogId;
        listing.item = item.toItem();
        listing.listedDate = new Date(listedDate.getTime());
        // the name part of the MemberName is filled in by ItemManager
        listing.creator = new MemberName(null, item.creatorId);
        listing.flowCost = flowCost;
        listing.goldCost = goldCost;
        listing.rarity = rarity;
        listing.purchases = purchases;
        listing.returns = returns;
        return listing;
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
