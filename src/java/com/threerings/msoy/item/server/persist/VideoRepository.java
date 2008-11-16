//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

// TODO: remove after 2008-11-12
import java.util.List;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.DataMigration;
// END: TODO

/**
 * Manages the persistent store of {@link VideoRecord} items.
 */
@Singleton
public class VideoRepository extends ItemRepository<VideoRecord>
{
    @Entity(name="VideoTagRecord")
    public static class VideoTagRecord extends TagRecord
    {
    }

    @Entity(name="VideoTagHistoryRecord")
    public static class VideoTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public VideoRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    // TEMP
    public void addYouTubeMigration (DataMigration mig)
    {
        registerMigration(mig);
    }

    // TODO: remove after 2008-11-12
    public List<VideoRecord> loadLegacyYouTube ()
    {
        return findAll(VideoRecord.class, new Where(VideoRecord.VIDEO_MIME_TYPE_C, 34));
    }

    @Override
    protected Class<VideoRecord> getItemClass ()
    {
        return VideoRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(VideoCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(VideoCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(VideoRatingRecord.class);
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
