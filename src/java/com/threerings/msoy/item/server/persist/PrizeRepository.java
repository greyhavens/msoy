//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link PrizeRecord} items.
 */
@Singleton
public class PrizeRepository extends ItemRepository<PrizeRecord>
{
    @Entity(name="PrizeTagRecord")
    public static class PrizeTagRecord extends TagRecord
    {
    }

    @Entity(name="PrizeTagHistoryRecord")
    public static class PrizeTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public PrizeRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<PrizeRecord> getItemClass ()
    {
        return PrizeRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(PrizeCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(PrizeCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return _ratingRepo.coerceRating(PrizeRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new PrizeTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new PrizeTagHistoryRecord();
    }
}
