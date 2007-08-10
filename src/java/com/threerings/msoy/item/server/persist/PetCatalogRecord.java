//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Pet. */
public class PetCatalogRecord extends CatalogRecord<PetRecord>
{
    public PetCatalogRecord ()
    {
        super();
    }

    protected PetCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PetCatalogRecord}
     * with the supplied key values.
     */
    public static Key<PetCatalogRecord> getKey (int itemId)
    {
        return new Key<PetCatalogRecord>(
                PetCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
