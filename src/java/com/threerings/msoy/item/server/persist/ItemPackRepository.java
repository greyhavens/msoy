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
 * Manages the persistent store of {@link ItemPackRecord} items.
 */
@Singleton
public class ItemPackRepository extends ItemRepository<ItemPackRecord>
{
    @Entity(name="ItemPackTagRecord")
    public static class ItemPackTagRecord extends TagRecord
    {
    }

    @Entity(name="ItemPackTagHistoryRecord")
    public static class ItemPackTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public ItemPackRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<ItemPackRecord> getItemClass ()
    {
        return ItemPackRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(ItemPackCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(ItemPackCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return _ratingRepo.coerceRating(ItemPackRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new ItemPackTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new ItemPackTagHistoryRecord();
    }
}
