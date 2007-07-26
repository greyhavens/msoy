//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Furniture} items.
 */
public class FurnitureRepository extends ItemRepository<
    FurnitureRecord,
    FurnitureCloneRecord,
    FurnitureCatalogRecord,
    FurnitureRatingRecord>
{
    @Entity(name="FurnitureTagRecord")
    public static class FurnitureTagRecord extends TagRecord
    {
    }

    @Entity(name="FurnitureTagHistoryRecord")
    public static class FurnitureTagHistoryRecord extends TagHistoryRecord
    {
    }

    public FurnitureRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<FurnitureRecord> getItemClass ()
    {
        return FurnitureRecord.class;
    }
    
    @Override
    protected Class<FurnitureCatalogRecord> getCatalogClass ()
    {
        return FurnitureCatalogRecord.class;
    }

    @Override
    protected Class<FurnitureCloneRecord> getCloneClass ()
    {
        return FurnitureCloneRecord.class;
    }
    
    @Override
    protected Class<FurnitureRatingRecord> getRatingClass ()
    {
        return FurnitureRatingRecord.class;
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
