//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Games. */
public class GameCatalogRecord extends CatalogRecord<GameRecord>
{
    public GameCatalogRecord ()
    {
        super();
    }

    protected GameCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameCatalogRecord}
     * with the supplied key values.
     */
    public static Key<GameCatalogRecord> getKey (int itemId)
    {
        return new Key<GameCatalogRecord>(
                GameCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
