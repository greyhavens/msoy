//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

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
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the audio media. */
    public byte[] audioMediaHash;

    /** The MIME type of the {@link #audioMediaHash} media. */
    public byte audioMimeType;

    public AudioRecord ()
    {
        super();
    }

    protected AudioRecord (Audio audio)
    {
        super(audio);

        if (audio.audioMedia != null) {
            audioMediaHash = audio.audioMedia.hash;
            audioMimeType = audio.audioMedia.mimeType;
        }
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.AUDIO;
    }

    @Override
    protected Item createItem ()
    {
        Audio object = new Audio();
        object.audioMedia = audioMediaHash == null ? null :
            new MediaDesc(audioMediaHash, audioMimeType);
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AudioRecord}
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
