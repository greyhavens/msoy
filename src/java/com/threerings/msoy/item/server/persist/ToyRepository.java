//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link Toy} items.
 */
public class ToyRepository extends ItemRepository<
    ToyRecord,
    ToyCloneRecord,
    ToyCatalogRecord,
    ToyRatingRecord>
{
    @Entity(name="ToyTagRecord")
    public static class ToyTagRecord extends TagRecord
    {
    }

    @Entity(name="ToyTagHistoryRecord")
    public static class ToyTagHistoryRecord extends TagHistoryRecord
    {
    }

    public ToyRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<ToyRecord> getItemClass ()
    {
        return ToyRecord.class;
    }

    @Override
    protected Class<ToyCatalogRecord> getCatalogClass ()
    {
        return ToyCatalogRecord.class;
    }

    @Override
    protected Class<ToyCloneRecord> getCloneClass ()
    {
        return ToyCloneRecord.class;
    }

    @Override
    protected Class<ToyRatingRecord> getRatingClass ()
    {
        return ToyRatingRecord.class;
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
