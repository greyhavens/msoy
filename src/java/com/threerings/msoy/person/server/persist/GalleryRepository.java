//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.IsNull;
import com.samskivert.jdbc.depot.operator.Logic.And;

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
                       new Where(new Equals(GalleryRecord.OWNER_ID_C, memberId)));
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
    public GalleryRecord insertGallery (int ownerId, String name, String description,
                                        int[] photoItemIds)
    {
        GalleryRecord gallery = new GalleryRecord();
        gallery.ownerId = ownerId;
        gallery.name = name;
        gallery.description = description;
        gallery.photoItemIds = photoItemIds;
        gallery.lastModified = currentTimestamp();
        insert(gallery);
        return gallery;
    }

    /**
     * Updates the specified gallery.
     */
    public void updateGallery (int galleryId, String name, String description, int[] photoItemIds)
    {
        updatePartial(GalleryRecord.getKey(galleryId),
                      GalleryRecord.NAME, name,
                      GalleryRecord.DESCRIPTION, description,
                      GalleryRecord.PHOTO_ITEM_IDS, photoItemIds,
                      GalleryRecord.LAST_MODIFIED, currentTimestamp());
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
