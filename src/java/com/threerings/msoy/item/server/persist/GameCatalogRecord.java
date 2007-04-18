//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.msoy.item.data.gwt.CatalogListing;

/** Catalog Records for Games. */
@Entity
@Table
public class GameCatalogRecord extends CatalogRecord<GameRecord>
{
    public GameCatalogRecord ()
    {
        super();
    }

    protected GameCatalogRecord (CatalogListing listing)
    {
        super(listing);
    }
}
