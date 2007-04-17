//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.all.CatalogListing;

/** Catalog Records for Decor. */
@Entity
@Table
public class DecorCatalogRecord extends CatalogRecord<DecorRecord>
{
    public DecorCatalogRecord ()
    {
        super();
    }

    protected DecorCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
