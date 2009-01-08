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
    public static final Class<AudioRecord> _R = AudioRecord.class;
    public static final ColumnExp AUDIO_MEDIA_HASH = colexp(_R, "audioMediaHash");
    public static final ColumnExp AUDIO_MIME_TYPE = colexp(_R, "audioMimeType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
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
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
