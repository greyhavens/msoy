//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

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
