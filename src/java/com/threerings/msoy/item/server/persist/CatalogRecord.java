//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Transient;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.money.data.all.Currency;

/**
 * Represents a catalog listing of an item.
 */
@Entity(indices={
    @Index(name="listedItemIndex", fields={ CatalogRecord.LISTED_ITEM_ID } ),
    @Index(name="listDateIndex", fields={ CatalogRecord.LISTED_DATE } ),
    @Index(name="purchasesIndex", fields={ CatalogRecord.PURCHASES } ),
    @Index(name="faveCountIndex", fields={ CatalogRecord.FAVORITE_COUNT } )
})
public abstract class CatalogRecord extends PersistentRecord
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

    /** The column identifier for the {@link #currency} field. */
    public static final String CURRENCY = "currency";

    /** The column identifier for the {@link #cost} field. */
    public static final String COST = "cost";

    /** The column identifier for the {@link #pricing} field. */
    public static final String PRICING = "pricing";

    /** The column identifier for the {@link #salesTarget} field. */
    public static final String SALES_TARGET = "salesTarget";

    /** The column identifier for the {@link #purchases} field. */
    public static final String PURCHASES = "purchases";

    /** The column identifier for the {@link #returns} field. */
    public static final String RETURNS = "returns";

    /** The column identifier for the {@link #favoriteCount} field. */
    public static final String FAVORITE_COUNT = "favoriteCount";
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 11;

    /** Converts this record to a runtime record. See {@link #toListingCard} for caveats.  */
    public static final Function<CatalogRecord,ListingCard> TO_CARD =
        new Function<CatalogRecord,ListingCard>() {
        public ListingCard apply (CatalogRecord record) {
            return record.toListingCard();
        }
    };

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

    /** The type of currency this item is listed for. */
    public byte currency;

    /** The cost to purchase a clone of this item. */
    public int cost;

    /** The pricing of this item; {@see CatalogListing#pricing}. */
    public int pricing;

    /** The number of unit sales after which to adjust the price or delist this item. */
    public int salesTarget;

    /** The number of times this item has been purchased. */
    public int purchases;

    /** The number of times this item has been returned. */
    public int returns;

    /** The number of people who consider the listed item a favorite. */
    public int favoriteCount;

    /** A reference to the listed item. This value is not persisted. */
    @Transient
    public ItemRecord item;

    /**
     * Creates a runtime record from this persistent record.
     */
    public CatalogListing toListing ()
    {
        CatalogListing listing = new CatalogListing();
        listing.catalogId = catalogId;
        if (item != null) {
            listing.detail = new ItemDetail();
            listing.detail.item = item.toItem();
        }
        listing.originalItemId = originalItemId;
        listing.listedDate = new Date(listedDate.getTime());
        listing.flowCost = cost;
        listing.goldCost = 0; // TODO
        listing.pricing = pricing;
        listing.salesTarget = salesTarget;
        listing.purchases = purchases;
        listing.returns = returns;
        listing.favoriteCount = favoriteCount;
        return listing;
    }

    /**
     * Creates a runtime card record from this persistent record. The caller is responsible for
     * looking up and filling in the member names of the creators. This method inserts a blank name
     * with the appropriate id to make that process easier.
     */
    public ListingCard toListingCard ()
    {
        ListingCard card = new ListingCard();
        card.itemType = item.getType();
        card.catalogId = catalogId;
        card.name = item.name;
        card.thumbMedia = item.getThumbMediaDesc();
        card.creator = new MemberName(null, item.creatorId); // name filled in by caller
        card.descrip = item.description;
        card.remixable = item.isRemixable();
        card.rating = item.rating;
        card.currency = Currency.fromByte(currency);
        card.cost = cost;
        return card;
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }
}
