//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link PhotoRecord} items.
 */
public class PhotoRepository extends ItemRepository<
    PhotoRecord,
    PhotoCloneRecord,
    PhotoCatalogRecord,
    PhotoRatingRecord>
{
    @Entity(name="PhotoTagRecord")
    public static class PhotoTagRecord extends TagRecord
    {
    }

    @Entity(name="PhotoTagHistoryRecord")
    public static class PhotoTagHistoryRecord extends TagHistoryRecord
    {
    }

    public PhotoRepository (PersistenceContext ctx)
    {
        super(ctx);
        _ctx.registerMigration(getItemClass(), new EntityMigration.Drop(16003, "suiteId"));
    }

    @Override
    protected Class<PhotoRecord> getItemClass ()
    {
        return PhotoRecord.class;
    }
    
    @Override
    protected Class<PhotoCatalogRecord> getCatalogClass ()
    {
        return PhotoCatalogRecord.class;
    }

    @Override
    protected Class<PhotoCloneRecord> getCloneClass ()
    {
        return PhotoCloneRecord.class;
    }

    @Override
    protected Class<PhotoRatingRecord> getRatingClass ()
    {
        return PhotoRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new PhotoTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new PhotoTagHistoryRecord();
    }
}
