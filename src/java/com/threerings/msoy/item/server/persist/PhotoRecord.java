//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;

/**
 * Represents an uploaded photograph for display in albums or for use as a
 * profile picture.
 */
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

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(PhotoRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(PhotoRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #flagged} field. */
    public static final ColumnExp FLAGGED_C =
        new ColumnExp(PhotoRecord.class, FLAGGED);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(PhotoRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(PhotoRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(PhotoRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(PhotoRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(PhotoRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(PhotoRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(PhotoRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(PhotoRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(PhotoRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(PhotoRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(PhotoRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(PhotoRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(PhotoRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(PhotoRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(PhotoRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(PhotoRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(PhotoRecord.class, FURNI_CONSTRAINT);
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

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.PHOTO;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Photo photo = (Photo)item;
        if (photo.photoMedia != null) {
            photoMediaHash = photo.photoMedia.hash;
            photoMimeType = photo.photoMedia.mimeType;
            photoConstraint = photo.photoMedia.constraint;
        }
        photoWidth = photo.photoWidth;
        photoHeight = photo.photoHeight;
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return photoMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return photoMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        photoMediaHash = hash;
    }

    @Override // from ItemRecord
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
     * Create and return a primary {@link Key} to identify a {@link PhotoRecord}
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
