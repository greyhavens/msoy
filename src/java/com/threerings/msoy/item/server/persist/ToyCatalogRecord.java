//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Toy. */
public class ToyCatalogRecord extends CatalogRecord<ToyRecord>
{
    public ToyCatalogRecord ()
    {
        super();
    }

    protected ToyCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ToyCatalogRecord}
     * with the supplied key values.
     */
    public static Key<ToyCatalogRecord> getKey (int itemId)
    {
        return new Key<ToyCatalogRecord>(
                ToyCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
