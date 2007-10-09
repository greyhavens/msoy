//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link VideoRecord} items.
 */
public class VideoRepository extends ItemRepository<
    VideoRecord,
    VideoCloneRecord,
    VideoCatalogRecord,
    VideoRatingRecord>
{
    @Entity(name="VideoTagRecord")
    public static class VideoTagRecord extends TagRecord
    {
    }

    @Entity(name="VideoTagHistoryRecord")
    public static class VideoTagHistoryRecord extends TagHistoryRecord
    {
    }

    public VideoRepository (PersistenceContext ctx)
    {
        super(ctx);
        _ctx.registerMigration(getItemClass(), new EntityMigration.Drop(16001, "suiteId"));
    }

    @Override
    protected Class<VideoRecord> getItemClass ()
    {
        return VideoRecord.class;
    }
    
    @Override
    protected Class<VideoCatalogRecord> getCatalogClass ()
    {
        return VideoCatalogRecord.class;
    }

    @Override
    protected Class<VideoCloneRecord> getCloneClass ()
    {
        return VideoCloneRecord.class;
    }

    @Override
    protected Class<VideoRatingRecord> getRatingClass ()
    {
        return VideoRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new VideoTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new VideoTagHistoryRecord();
    }
}
