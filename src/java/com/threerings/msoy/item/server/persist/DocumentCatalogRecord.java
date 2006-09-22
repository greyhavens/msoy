//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.threerings.msoy.item.web.CatalogListing;

/** Catalog Records for Documents. */
@Entity
@Table
public class DocumentCatalogRecord extends CatalogRecord<DocumentRecord>
{
    public DocumentCatalogRecord ()
    {
        super();
    }

    public DocumentCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }

}
