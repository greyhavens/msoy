//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Documents. */
public class DocumentCatalogRecord extends CatalogRecord<DocumentRecord>
{
    public DocumentCatalogRecord ()
    {
        super();
    }

    protected DocumentCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #DocumentCatalogRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCatalogRecord> getKey (int itemId)
    {
        return new Key<DocumentCatalogRecord>(
                DocumentCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
