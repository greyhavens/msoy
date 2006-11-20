//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Represents an uploaded piece of audio.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="AUDIO")
public class AudioRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

    public static final String AUDIO_MEDIA_HASH = "audioMediaHash";
    public static final String AUDIO_MIME_TYPE = "audioMimeType";

    /** A hash code identifying the audio media. */
    @Column(nullable=false)
    public byte[] audioMediaHash;

    /** The MIME type of the {@link #audioMediaHash} media. */
    @Column(nullable=false)
    public byte audioMimeType;

    /** A description of this audio (max length 255 characters). */
    @Column(nullable=false)
    public String description;

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
        description = audio.description;
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
        object.description = description;
        return object;
    }
}
