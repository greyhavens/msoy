//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;

/**
 * The record of a gallery and the IDs of its photo contents.
 *
 * @author mdb
 * @author mjensen
 */
@Entity
public class GalleryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GalleryRecord> _R = GalleryRecord.class;
    public static final ColumnExp GALLERY_ID = colexp(_R, "galleryId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp PHOTO_ITEM_IDS = colexp(_R, "photoItemIds");
    public static final ColumnExp LAST_MODIFIED = colexp(_R, "lastModified");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** A unique identifier for this gallery. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int galleryId;

    /** The member id of the owner of this gallery. */
    @Index(name="ixOwnerId")
    public int ownerId;

    /** The name of this gallery or null if it is the "Photos of Me" gallery. */
    @Column(length=Gallery.MAX_NAME_LENGTH, nullable=true)
    public String name;

    /** A brief description of the gallery */
    @Column(length=Gallery.MAX_DESCRIPTION_LENGTH, nullable=true)
    public String description;

    /** An ordered list of photo item ids that make up this gallery. */
    @Column(length=2048) // results in maximum gallery size of 512 images
    public int[] photoItemIds;

    /** The time at which this gallery was last modified. */
    public Timestamp lastModified;

    /** A hash code identifying the media used to display this item's thumbnail representation. */
    @Column(nullable = true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Gallery toGallery ()
    {
        Gallery gallery = new Gallery();
        gallery.galleryId = galleryId;
        gallery.name = name;
        gallery.description = description;
        gallery.lastModified = new Date(lastModified.getTime());
        gallery.thumbMedia = new MediaDesc(thumbMediaHash, thumbMimeType, thumbConstraint);
        return gallery;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public static GalleryRecord fromGallery (Gallery gallery)
    {
        GalleryRecord record = new GalleryRecord();
        record.galleryId = gallery.galleryId;
        record.name = gallery.name;
        record.description = gallery.description == null ? "" : gallery.description;
        if (gallery.lastModified != null) {
            record.lastModified = new Timestamp(gallery.lastModified.getTime());
        }
        if (gallery.thumbMedia != null) {
            record.thumbMediaHash = gallery.thumbMedia.hash;
            record.thumbMimeType = gallery.thumbMedia.mimeType;
            record.thumbConstraint = gallery.thumbMedia.constraint;
        }
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GalleryRecord}
     * with the supplied key values.
     */
    public static Key<GalleryRecord> getKey (int galleryId)
    {
        return new Key<GalleryRecord>(
                GalleryRecord.class,
                new ColumnExp[] { GALLERY_ID },
                new Comparable[] { galleryId });
    }
    // AUTO-GENERATED: METHODS END
}
