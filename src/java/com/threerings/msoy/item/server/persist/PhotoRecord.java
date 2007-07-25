//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

/**
 * Represents an uploaded photograph for display in albums or for use as a
 * profile picture.
 */
@Entity
@Table
@TableGenerator(name="itemId", pkColumnValue="PHOTO")
public class PhotoRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #photoMediaHash} field. */
    public static final String PHOTO_MEDIA_HASH = "photoMediaHash";

    /** The qualified column identifier for the {@link #photoMediaHash} field. */
    public static final ColumnExp PHOTO_MEDIA_HASH_C =
        new ColumnExp(PhotoRecord.class, PHOTO_MEDIA_HASH);

    /** The column identifier for the {@link #photoMimeType} field. */
    public static final String PHOTO_MIME_TYPE = "photoMimeType";

    /** The qualified column identifier for the {@link #photoMimeType} field. */
    public static final ColumnExp PHOTO_MIME_TYPE_C =
        new ColumnExp(PhotoRecord.class, PHOTO_MIME_TYPE);

    /** The column identifier for the {@link #photoConstraint} field. */
    public static final String PHOTO_CONSTRAINT = "photoConstraint";

    /** The qualified column identifier for the {@link #photoConstraint} field. */
    public static final ColumnExp PHOTO_CONSTRAINT_C =
        new ColumnExp(PhotoRecord.class, PHOTO_CONSTRAINT);

    /** The column identifier for the {@link #photoWidth} field. */
    public static final String PHOTO_WIDTH = "photoWidth";

    /** The qualified column identifier for the {@link #photoWidth} field. */
    public static final ColumnExp PHOTO_WIDTH_C =
        new ColumnExp(PhotoRecord.class, PHOTO_WIDTH);

    /** The column identifier for the {@link #photoHeight} field. */
    public static final String PHOTO_HEIGHT = "photoHeight";

    /** The qualified column identifier for the {@link #photoHeight} field. */
    public static final ColumnExp PHOTO_HEIGHT_C =
        new ColumnExp(PhotoRecord.class, PHOTO_HEIGHT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the photo media. */
    public byte[] photoMediaHash;

    /** The MIME type of the {@link #photoMediaHash} media. */
    public byte photoMimeType;

    /** The size constraint on the {@link #photoMediaHash} media. */
    public byte photoConstraint;

    /** The width (in pixels) of the main photo media. */
    public int photoWidth;

    /** The height (in pixels) of the main photo media. */
    public int photoHeight;

    public PhotoRecord ()
    {
        super();
    }

    protected PhotoRecord (Photo photo)
    {
        super(photo);

        if (photo.photoMedia != null) {
            photoMediaHash = photo.photoMedia.hash;
            photoMimeType = photo.photoMedia.mimeType;
            photoConstraint = photo.photoMedia.constraint;
        }
        photoWidth = photo.photoWidth;
        photoHeight = photo.photoHeight;
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.PHOTO;
    }

    @Override
    protected Item createItem ()
    {
        Photo object = new Photo();
        object.photoMedia = (photoMediaHash == null) ?
            null : new MediaDesc(photoMediaHash, photoMimeType, photoConstraint);
        object.photoWidth = photoWidth;
        object.photoHeight = photoHeight;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PhotoRecord}
     * with the supplied key values.
     */
    public static Key<PhotoRecord> getKey (int itemId)
    {
        return new Key<PhotoRecord>(
                PhotoRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
