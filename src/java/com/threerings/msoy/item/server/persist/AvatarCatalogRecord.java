//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Avatars. */
public class AvatarCatalogRecord extends CatalogRecord<AvatarRecord>
{
    public AvatarCatalogRecord ()
    {
        super();
    }

    protected AvatarCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AvatarCatalogRecord}
     * with the supplied key values.
     */
    public static Key<AvatarCatalogRecord> getKey (int itemId)
    {
        return new Key<AvatarCatalogRecord>(
                AvatarCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
