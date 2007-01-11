//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.Transient;

import com.threerings.io.Streamable;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.web.data.MemberName;

/**
 * Represents a catalog listing of an item.
 */
@Entity
@Table
public abstract class CatalogRecord<T extends ItemRecord>
    implements Streamable, Serializable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String ITEM_ID = "itemId";
    public static final String LISTED_DATE = "listedDate";

    /** A reference to the listed item. This value is not persisted. */
    @Transient
    public ItemRecord item;

    /** The ID of the listed item. */
    @Id
    public int itemId;

    /** The in time this item was listed in the catalog. */
    public Timestamp listedDate;

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
        listing.price = 0; // TODO
        return listing;
    }
}
