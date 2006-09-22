//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.threerings.msoy.item.web.CatalogListing;

/** Catalog Records for Games. */
@Entity
@Table
public class GameCatalogRecord extends CatalogRecord<GameRecord>
{
    public GameCatalogRecord ()
    {
        super();
    }

    public GameCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
