//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Videos. */
@Entity
@Table
public class VideoCatalogRecord extends CatalogRecord<VideoRecord>
{
    public VideoCatalogRecord ()
    {
        super();
    }

    protected VideoCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #VideoCatalogRecord}
     * with the supplied key values.
     */
    public static Key<VideoCatalogRecord> getKey (int itemId)
    {
        return new Key<VideoCatalogRecord>(
                VideoCatalogRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
