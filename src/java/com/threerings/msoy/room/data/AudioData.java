//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;

/**
 * Contains immutable information on background audio in a scene.
 */
public class AudioData extends SimpleStreamableObject
{
    /** Identifies the id of the item that was used to create this. */
    public int itemId;

    /** Media contained in the audio item. */
    public MediaDesc media;

    /** Audio volume. */
//    public float volume;

    /**
     * Constructor.
     */
    public AudioData ()
    {
    }

    public AudioData (int itemId, MediaDesc media)
    {
        this.itemId = itemId;
        this.media = media;
    }
}
