//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Audios. */
public class AudioCatalogRecord extends CatalogRecord<AudioRecord>
{
    public AudioCatalogRecord ()
    {
        super();
    }

    protected AudioCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AudioCatalogRecord}
     * with the supplied key values.
     */
    public static Key<AudioCatalogRecord> getKey (int itemId)
    {
        return new Key<AudioCatalogRecord>(
                AudioCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
