//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.all.CatalogListing;

/** Catalog Records for Furniture. */
@Entity
@Table
public class FurnitureCatalogRecord extends CatalogRecord<FurnitureRecord>
{
    public FurnitureCatalogRecord ()
    {
        super();
    }

    protected FurnitureCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
