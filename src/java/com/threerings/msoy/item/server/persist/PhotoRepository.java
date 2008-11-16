//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link PhotoRecord} items.
 */
@Singleton
public class PhotoRepository extends ItemRepository<PhotoRecord>
{
    @Entity(name="PhotoTagRecord")
    public static class PhotoTagRecord extends TagRecord
    {
    }

    @Entity(name="PhotoTagHistoryRecord")
    public static class PhotoTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public PhotoRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<PhotoRecord> getItemClass ()
    {
        return PhotoRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(PhotoCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(PhotoCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(PhotoRatingRecord.class);
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
