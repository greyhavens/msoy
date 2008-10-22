//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link PropRecord} items.
 */
@Singleton
public class PropRepository extends ItemRepository<PropRecord>
{
    @Entity(name="PropTagRecord")
    public static class PropTagRecord extends TagRecord
    {
    }

    @Entity(name="PropTagHistoryRecord")
    public static class PropTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public PropRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<PropRecord> getItemClass ()
    {
        return PropRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(PropCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(PropCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(PropRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new PropTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new PropTagHistoryRecord();
    }
}
