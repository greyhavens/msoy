//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents audio data.
 */
public class Audio extends Item
{
    /** The audio media.*/
    public MediaDesc audioMedia;

    // @Override // from Item
    public byte getType ()
    {
        return AUDIO;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (audioMedia != null) && audioMedia.isAudio() &&
            nonBlank(name);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia(); // TODO: support album art?
    }
}
