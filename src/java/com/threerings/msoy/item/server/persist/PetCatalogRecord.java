//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.all.CatalogListing;

/** Catalog Records for Pet. */
@Entity
@Table
public class PetCatalogRecord extends CatalogRecord<PetRecord>
{
    public PetCatalogRecord ()
    {
        super();
    }

    protected PetCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
