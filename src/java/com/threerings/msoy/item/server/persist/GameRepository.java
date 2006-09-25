//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Game} items.
 */
public class GameRepository extends ItemRepository<GameRecord>
{
    public GameRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<GameRecord> getItemClass () {
        return GameRecord.class;
    }
    
    @Override
    protected Class<? extends CatalogRecord<GameRecord>> getCatalogClass ()
    {
        return GameCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<GameRecord>> getCloneClass ()
    {
        return GameCloneRecord.class;
    }
}
