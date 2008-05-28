//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link PropRecord} items.
 */
public class PropRepository extends ItemRepository<
    PropRecord,
    PropCloneRecord,
    PropCatalogRecord,
    PropRatingRecord>
{
    @Entity(name="PropTagRecord")
    public static class PropTagRecord extends TagRecord
    {
    }

    @Entity(name="PropTagHistoryRecord")
    public static class PropTagHistoryRecord extends TagHistoryRecord
    {
    }

    public PropRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<PropRecord> getItemClass ()
    {
        return PropRecord.class;
    }

    @Override
    protected Class<PropCatalogRecord> getCatalogClass ()
    {
        return PropCatalogRecord.class;
    }

    @Override
    protected Class<PropCloneRecord> getCloneClass ()
    {
        return PropCloneRecord.class;
    }

    @Override
    protected Class<PropRatingRecord> getRatingClass ()
    {
        return PropRatingRecord.class;
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
