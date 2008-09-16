//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.google.common.base.Function;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

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
    // AUTO-GENERATED: FIELDS END

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int galleryId;

    public int ownerId;

    /** The name of this gallery or null if it is the "Photos of Me" gallery. */
    @Column(length=Gallery.MAX_NAME_LENGTH, nullable=true)
    public String name;

    @Column(length=2048) // results in maximum gallery size of 512 images
    public int[] photoItemIds;

    public Timestamp lastModified;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GalleryRecord}
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
