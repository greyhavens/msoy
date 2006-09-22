//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.threerings.msoy.item.web.CatalogListing;

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
