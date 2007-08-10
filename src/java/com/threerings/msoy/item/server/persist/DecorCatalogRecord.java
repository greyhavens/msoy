//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Decor. */
public class DecorCatalogRecord extends CatalogRecord<DecorRecord>
{
    public DecorCatalogRecord ()
    {
        super();
    }

    protected DecorCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DecorCatalogRecord}
     * with the supplied key values.
     */
    public static Key<DecorCatalogRecord> getKey (int itemId)
    {
        return new Key<DecorCatalogRecord>(
                DecorCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
