//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Photos. */
public class PhotoCatalogRecord extends CatalogRecord<PhotoRecord>
{
    public PhotoCatalogRecord ()
    {
        super();
    }

    protected PhotoCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PhotoCatalogRecord}
     * with the supplied key values.
     */
    public static Key<PhotoCatalogRecord> getKey (int itemId)
    {
        return new Key<PhotoCatalogRecord>(
                PhotoCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
