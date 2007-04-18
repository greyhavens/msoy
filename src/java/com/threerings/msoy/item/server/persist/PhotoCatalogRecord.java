//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Photos. */
@Entity
@Table
public class PhotoCatalogRecord extends CatalogRecord<PhotoRecord>
{
    public PhotoCatalogRecord ()
    {
        super();
    }

    protected PhotoCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
