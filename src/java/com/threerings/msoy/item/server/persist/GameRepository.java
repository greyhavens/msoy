//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of Game items.
 */
@Singleton
public class GameRepository extends ItemRepository<GameRecord>
{
    @Entity(name="GameMogMarkRecord")
    public static class GameMogMarkRecord extends MogMarkRecord
    {
    }

    @Entity(name="GameTagRecord")
    public static class GameTagRecord extends TagRecord
    {
    }

    @Entity(name="GameTagHistoryRecord")
    public static class GameTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public GameRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override // from ItemRepository
    protected Class<GameRecord> getItemClass ()
    {
        return GameRecord.class;
    }

    @Override // from ItemRepository
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(GameCatalogRecord.class);
    }

    @Override // from ItemRepository
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(GameCloneRecord.class);
    }

    @Override // from ItemRepository
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(GameRatingRecord.class);
    }

    @Override
    protected MogMarkRecord createMogMarkRecord ()
    {
        return new GameMogMarkRecord();
    }

    @Override // from ItemRepository
    protected TagRecord createTagRecord ()
    {
        return new GameTagRecord();
    }

    @Override // from ItemRepository
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new GameTagHistoryRecord();
    }
}
