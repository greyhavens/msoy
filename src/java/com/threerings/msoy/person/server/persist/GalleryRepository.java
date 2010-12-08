//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.msoy.person.gwt.Gallery;

/**
 * Provides access to gallery persistence.
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
        return from(GalleryInfoRecord.class).where(GalleryRecord.OWNER_ID.eq(memberId)).
            descending(GalleryRecord.LAST_MODIFIED).select();
    }

    /**
     * Returns this member's "Photos of Me" gallery.
     */
    public GalleryRecord loadMeGallery (int memberId)
    {
        return from(GalleryRecord.class).where( // a null gallery name indicates the "Me" gallery
            GalleryRecord.OWNER_ID.eq(memberId), GalleryRecord.NAME.isNull()).load();
    }

    /**
     * Loads the specified gallery.
     */
    public GalleryRecord loadGallery (int galleryId)
    {
        return load(GalleryRecord.getKey(galleryId));
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
        return delete(GalleryRecord.getKey(galleryId));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        from(GalleryRecord.class).where(GalleryRecord.OWNER_ID.in(memberIds)).delete();
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
    }
}
