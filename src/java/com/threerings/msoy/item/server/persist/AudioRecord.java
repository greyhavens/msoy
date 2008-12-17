//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents an uploaded piece of audio.
 */
@TableGenerator(name="itemId", pkColumnValue="AUDIO")
public class AudioRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #audioMediaHash} field. */
    public static final String AUDIO_MEDIA_HASH = "audioMediaHash";

    /** The qualified column identifier for the {@link #audioMediaHash} field. */
    public static final ColumnExp AUDIO_MEDIA_HASH_C =
        new ColumnExp(AudioRecord.class, AUDIO_MEDIA_HASH);

    /** The column identifier for the {@link #audioMimeType} field. */
    public static final String AUDIO_MIME_TYPE = "audioMimeType";

    /** The qualified column identifier for the {@link #audioMimeType} field. */
    public static final ColumnExp AUDIO_MIME_TYPE_C =
        new ColumnExp(AudioRecord.class, AUDIO_MIME_TYPE);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(AudioRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(AudioRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(AudioRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(AudioRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(AudioRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(AudioRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(AudioRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(AudioRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(AudioRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(AudioRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(AudioRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(AudioRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(AudioRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(AudioRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(AudioRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(AudioRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(AudioRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(AudioRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(AudioRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the audio media. */
    public byte[] audioMediaHash;

    /** The MIME type of the {@link #audioMediaHash} media. */
    public byte audioMimeType;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.AUDIO;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Audio audio = (Audio)item;
        if (audio.audioMedia != null) {
            audioMediaHash = audio.audioMedia.hash;
            audioMimeType = audio.audioMedia.mimeType;
        }
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return audioMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return audioMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        audioMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Audio object = new Audio();
        object.audioMedia = audioMediaHash == null ? null :
            new MediaDesc(audioMediaHash, audioMimeType);
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AudioRecord}
     * with the supplied key values.
     */
    public static Key<AudioRecord> getKey (int itemId)
    {
        return new Key<AudioRecord>(
                AudioRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
