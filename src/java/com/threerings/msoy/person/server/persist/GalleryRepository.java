//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.threerings.msoy.person.gwt.Gallery;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals.IsNull;
import com.samskivert.depot.operator.Logic.And;

/**
 * Provides access to gallery persistence.
 *
 * @author mdb
 * @author mjensen
 */
@Singleton
public class GalleryRepository extends DepotRepository
{
    @Inject public GalleryRepository (PersistenceContext context)
    {
        super(context);
    }

    /**
     * Loads metadata on all galleries owned by the specified member.
     */
    public List<GalleryInfoRecord> loadGalleries (int memberId)
    {
        return findAll(GalleryInfoRecord.class,
            new Where(new Equals(GalleryRecord.OWNER_ID_C, memberId)),
                      OrderBy.descending(GalleryRecord.LAST_MODIFIED_C));
    }

    /**
     * Returns this member's "Photos of Me" gallery.
     */
    public GalleryRecord loadMeGallery (int memberId)
    {
        return load(GalleryRecord.class,
                    new Where(new And(new Equals(GalleryRecord.OWNER_ID_C, memberId),
                                      // a null gallery name indicates the "Me" gallery
                                      new IsNull(GalleryRecord.NAME_C))));
    }

    /**
     * Loads the specified gallery.
     */
    public GalleryRecord loadGallery (int galleryId)
    {
        return load(GalleryRecord.class, GalleryRecord.GALLERY_ID, galleryId);
    }

    /**
     * Creates a new gallery.
     */
    public GalleryRecord insertGallery (int ownerId, Gallery gallery, int[] photoItemIds)
    {
        GalleryRecord galleryRecord = GalleryRecord.fromGallery(gallery);
        galleryRecord.lastModified = currentTimestamp();
        galleryRecord.photoItemIds = photoItemIds;
        galleryRecord.ownerId = ownerId;
        insert(galleryRecord);
        return galleryRecord;
    }

    /**
     * Updates the specified gallery.
     */
    public void updateGallery (Gallery gallery, int[] photoItemIds)
    {
        GalleryRecord galleryRecord = GalleryRecord.fromGallery(gallery);
        updatePartial(GalleryRecord.getKey(galleryRecord.galleryId),
                      GalleryRecord.NAME, galleryRecord.name,
                      GalleryRecord.DESCRIPTION, galleryRecord.description,
                      GalleryRecord.PHOTO_ITEM_IDS, photoItemIds,
                      GalleryRecord.LAST_MODIFIED, currentTimestamp(),
                      GalleryRecord.THUMB_MEDIA_HASH, galleryRecord.thumbMediaHash,
                      GalleryRecord.THUMB_MIME_TYPE, galleryRecord.thumbMimeType,
                      GalleryRecord.THUMB_CONSTRAINT, galleryRecord.thumbConstraint);
    }

    /**
     * Deletes the specified gallery.
     */
    public int deleteGallery (int galleryId)
    {
        return delete(GalleryRecord.class, galleryId);
    }

    /**
     * Returns the current time as a {@link Timestamp}.
     */
    protected static Timestamp currentTimestamp ()
    {
       return new Timestamp(System.currentTimeMillis());
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GalleryRecord.class);
        classes.add(GalleryInfoRecord.class);
    }
}
