//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;

/**
 * The record of a gallery and the IDs of its photo contents.
 *
 * @author mdb
 * @author mjensen
 */
@Entity(indices={ @Index(name="ixOwnerId", fields={GalleryRecord.OWNER_ID}) })
public class GalleryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #galleryId} field. */
    public static final String GALLERY_ID = "galleryId";

    /** The qualified column identifier for the {@link #galleryId} field. */
    public static final ColumnExp GALLERY_ID_C =
        new ColumnExp(GalleryRecord.class, GALLERY_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(GalleryRecord.class, OWNER_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(GalleryRecord.class, NAME);

    /** The column identifier for the {@link #description} field. */
    public static final String DESCRIPTION = "description";

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(GalleryRecord.class, DESCRIPTION);

    /** The column identifier for the {@link #photoItemIds} field. */
    public static final String PHOTO_ITEM_IDS = "photoItemIds";

    /** The qualified column identifier for the {@link #photoItemIds} field. */
    public static final ColumnExp PHOTO_ITEM_IDS_C =
        new ColumnExp(GalleryRecord.class, PHOTO_ITEM_IDS);

    /** The column identifier for the {@link #lastModified} field. */
    public static final String LAST_MODIFIED = "lastModified";

    /** The qualified column identifier for the {@link #lastModified} field. */
    public static final ColumnExp LAST_MODIFIED_C =
        new ColumnExp(GalleryRecord.class, LAST_MODIFIED);

    /** The column identifier for the {@link #thumbMediaHash} field. */
    public static final String THUMB_MEDIA_HASH = "thumbMediaHash";

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(GalleryRecord.class, THUMB_MEDIA_HASH);

    /** The column identifier for the {@link #thumbMimeType} field. */
    public static final String THUMB_MIME_TYPE = "thumbMimeType";

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(GalleryRecord.class, THUMB_MIME_TYPE);

    /** The column identifier for the {@link #thumbConstraint} field. */
    public static final String THUMB_CONSTRAINT = "thumbConstraint";

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(GalleryRecord.class, THUMB_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** A unique identifier for this gallery. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int galleryId;

    /** The member id of the owner of this gallery. */
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
                new String[] { GALLERY_ID },
                new Comparable[] { galleryId });
    }
    // AUTO-GENERATED: METHODS END
}
