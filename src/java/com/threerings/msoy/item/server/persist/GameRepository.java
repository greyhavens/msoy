//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.EntityMigration;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Manages the persistent store of {@link Game} items.
 */
public class GameRepository extends ItemRepository<
        GameRecord,
        GameCloneRecord,
        GameCatalogRecord,
        GameRatingRecord>
{
    @Entity(name="GameTagRecord")
    public static class GameTagRecord extends TagRecord
    {
    }

    @Entity(name="GameTagHistoryRecord")
    public static class GameTagHistoryRecord extends TagHistoryRecord
    {
    }

    public GameRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP
        _ctx.registerMigration(GameRecord.class, new EntityMigration.Drop(8007, "gameType"));
        _ctx.registerMigration(GameRecord.class, new EntityMigration.Drop(8007, "minPlayers"));
        _ctx.registerMigration(GameRecord.class, new EntityMigration.Drop(8007, "maxPlayers"));
        _ctx.registerMigration(GameRecord.class, new EntityMigration.Drop(8007, "unwatchable"));
        // END TEMP
    }

    @Override
    protected Class<GameRecord> getItemClass ()
    {
        return GameRecord.class;
    }
    
    @Override
    protected Class<GameCatalogRecord> getCatalogClass ()
    {
        return GameCatalogRecord.class;
    }

    @Override
    protected Class<GameCloneRecord> getCloneClass ()
    {
        return GameCloneRecord.class;
    }
    
    @Override
    protected Class<GameRatingRecord> getRatingClass ()
    {
        return GameRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new GameTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new GameTagHistoryRecord();
    }
}
