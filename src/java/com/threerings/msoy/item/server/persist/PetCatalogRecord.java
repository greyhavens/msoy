//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.threerings.msoy.item.web.CatalogListing;

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
