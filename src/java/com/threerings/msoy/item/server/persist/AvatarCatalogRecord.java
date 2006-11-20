//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.web.CatalogListing;

/** Catalog Records for Avatars. */
@Entity
@Table
public class AvatarCatalogRecord extends CatalogRecord<AvatarRecord>
{
    public AvatarCatalogRecord ()
    {
        super();
    }

    protected AvatarCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
