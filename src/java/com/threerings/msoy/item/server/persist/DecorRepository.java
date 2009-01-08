//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Decor} items.
 */
@Singleton
public class DecorRepository extends ItemRepository<DecorRecord>
{
    @Entity(name="DecorTagRecord")
    public static class DecorTagRecord extends TagRecord
    {
    }

    @Entity(name="DecorTagHistoryRecord")
    public static class DecorTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public DecorRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<DecorRecord> getItemClass ()
    {
        return DecorRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(DecorCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(DecorCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(DecorRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new DecorTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new DecorTagHistoryRecord();
    }
}
