//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.all.CatalogListing;

/** Catalog Records for Audios. */
@Entity
@Table
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
}
