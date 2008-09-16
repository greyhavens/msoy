//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.util.Date;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

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
    public Date lastModified;

    /**
     * Creates a runtime record from this persistent record.
     */
    public Gallery toGallery ()
    {
        Gallery gallery = new Gallery();
        gallery.galleryId = galleryId;
        gallery.name = name;
        gallery.lastModified = new Date(lastModified.getTime());
        return gallery;
    }

}


