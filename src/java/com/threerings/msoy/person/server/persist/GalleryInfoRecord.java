//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Date;
import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

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
    public static final Class<GalleryInfoRecord> _R = GalleryInfoRecord.class;
    public static final ColumnExp GALLERY_ID = colexp(_R, "galleryId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp LAST_MODIFIED = colexp(_R, "lastModified");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
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
