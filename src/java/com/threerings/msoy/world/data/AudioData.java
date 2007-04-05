//
// $Id$

package com.threerings.msoy.world.data;

import com.samskivert.util.ObjectUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.web.Audio;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

/**
 * Contains information on background audio in a scene.
 */
public class AudioData extends SimpleStreamableObject
    implements Cloneable
{
    /** Identifies the id of the item that was used to create this. */
    public int itemId;

    /** Media contained in the audio item. */
    public MediaDesc media;

    /** Audio volume. */
    public float volume;
    
    /**
     * Helper function: specifies that this decor data structure has already been
     * populated from an Audio item object.
     */
    public boolean isInitialized ()
    {
        return itemId != 0;
    }
    
    // documentation inherited 
    @Override
    public boolean equals (Object other)
    {
        // just compare whether they refer to the same audio item
        return (other instanceof AudioData) && ((AudioData) other).itemId == this.itemId;
    }

    // documentation inherited
    @Override
    public String toString ()
    {
        return "Audio[itemId=" + itemId + ", media=" + media + "]";
    }

    // documentation inherited
    public Object clone ()
    {
        try {
            // perform a shallow copy of value attributes
            AudioData data = (AudioData) super.clone();
            // now do a copy of the media object (too bad MediaDesc can't be Cloneable...)
            if (media != null) {
                data.media = new MediaDesc(media.hash, media.mimeType, media.constraint);
            }
            return data;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);           // not going to happen
        }
    }

}
