//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.threerings.msoy.item.web.CatalogListing;

/** Catalog Records for Photos. */
@Entity
@Table
public class PhotoCatalogRecord extends CatalogRecord<PhotoRecord>
{
    public PhotoCatalogRecord ()
    {
        super();
    }

    public PhotoCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
