//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Furniture} items.
 */
@Singleton
public class FurnitureRepository extends ItemRepository<FurnitureRecord>
{
    @Entity(name="FurnitureTagRecord")
    public static class FurnitureTagRecord extends TagRecord
    {
    }

    @Entity(name="FurnitureTagHistoryRecord")
    public static class FurnitureTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public FurnitureRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<FurnitureRecord> getItemClass ()
    {
        return FurnitureRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(FurnitureCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(FurnitureCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(FurnitureRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new FurnitureTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new FurnitureTagHistoryRecord();
    }
}
