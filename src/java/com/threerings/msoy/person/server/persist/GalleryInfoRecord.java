//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Date;
import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.Gallery;

/**
 * This represents just the gallery meta data, id and name, needed by {@link
 * GalleryRepository#loadGalleries}, etc.
 *
 * @author mdb
 * @author mjensen
 */
@Entity @Computed(shadowOf=GalleryRecord.class)
public class GalleryInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #galleryId} field. */
    public static final String GALLERY_ID = "galleryId";

    /** The qualified column identifier for the {@link #galleryId} field. */
    public static final ColumnExp GALLERY_ID_C =
        new ColumnExp(GalleryInfoRecord.class, GALLERY_ID);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(GalleryInfoRecord.class, NAME);

    /** The column identifier for the {@link #lastModified} field. */
    public static final String LAST_MODIFIED = "lastModified";

    /** The qualified column identifier for the {@link #lastModified} field. */
    public static final ColumnExp LAST_MODIFIED_C =
        new ColumnExp(GalleryInfoRecord.class, LAST_MODIFIED);

    /** The column identifier for the {@link #thumbMediaHash} field. */
    public static final String THUMB_MEDIA_HASH = "thumbMediaHash";

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(GalleryInfoRecord.class, THUMB_MEDIA_HASH);

    /** The column identifier for the {@link #thumbMimeType} field. */
    public static final String THUMB_MIME_TYPE = "thumbMimeType";

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(GalleryInfoRecord.class, THUMB_MIME_TYPE);

    /** The column identifier for the {@link #thumbConstraint} field. */
    public static final String THUMB_CONSTRAINT = "thumbConstraint";

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(GalleryInfoRecord.class, THUMB_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    /** Converts persistent records into runtime records. */
    public static final Function<GalleryInfoRecord, Gallery> TO_GALLERY =
        new Function<GalleryInfoRecord, Gallery>() {
            public Gallery apply (GalleryInfoRecord record) {
                return record.toGallery();
            }
        };

    /** The id of the gallery in question. */
    public int galleryId;

    /** The name of the gallery in question. */
    public String name;

    /** The last modified time of the gallery in question. */
    public Timestamp lastModified;

    /** A hash code identifying the media used to display this item's thumbnail representation. */
    @Column(nullable = true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /**
     * Creates a runtime record from this persistent record.
     */
    public Gallery toGallery ()
    {
        Gallery gallery = new Gallery();
        gallery.galleryId = galleryId;
        gallery.name = name;
        gallery.lastModified = new Date(lastModified.getTime());
        gallery.thumbMedia = new MediaDesc(thumbMediaHash, thumbMimeType, thumbConstraint);
        return gallery;
    }
}
