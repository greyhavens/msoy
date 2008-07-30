//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link Toy} items.
 */
@Singleton
public class ToyRepository extends ItemRepository<ToyRecord>
{
    @Entity(name="ToyTagRecord")
    public static class ToyTagRecord extends TagRecord
    {
    }

    @Entity(name="ToyTagHistoryRecord")
    public static class ToyTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public ToyRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<ToyRecord> getItemClass ()
    {
        return ToyRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(ToyCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(ToyCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return coerceRating(ToyRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new ToyTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new ToyTagHistoryRecord();
    }
}
