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
 * Manages the persistent store of {@link TrophySourceRecord} items.
 */
@Singleton
public class TrophySourceRepository extends ItemRepository<TrophySourceRecord>
{
    @Entity(name="TrophySourceTagRecord")
    public static class TrophySourceTagRecord extends TagRecord
    {
    }

    @Entity(name="TrophySourceTagHistoryRecord")
    public static class TrophySourceTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public TrophySourceRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<TrophySourceRecord> getItemClass ()
    {
        return TrophySourceRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(TrophySourceCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(TrophySourceCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(TrophySourceRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new TrophySourceTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new TrophySourceTagHistoryRecord();
    }
}
