//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Furniture. */
public class FurnitureCatalogRecord extends CatalogRecord<FurnitureRecord>
{
    public FurnitureCatalogRecord ()
    {
        super();
    }

    protected FurnitureCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #FurnitureCatalogRecord}
     * with the supplied key values.
     */
    public static Key<FurnitureCatalogRecord> getKey (int itemId)
    {
        return new Key<FurnitureCatalogRecord>(
                FurnitureCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
