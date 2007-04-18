//
// $Id$

package com.threerings.msoy.item.server.persist;

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
}
