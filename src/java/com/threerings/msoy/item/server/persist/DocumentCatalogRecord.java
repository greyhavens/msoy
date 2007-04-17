//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.all.CatalogListing;

/** Catalog Records for Documents. */
@Entity
@Table
public class DocumentCatalogRecord extends CatalogRecord<DocumentRecord>
{
    public DocumentCatalogRecord ()
    {
        super();
    }

    protected DocumentCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

}
