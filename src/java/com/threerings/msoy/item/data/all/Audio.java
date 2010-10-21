//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.ConstrainedMediaDesc;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents audio data.
 */
public class Audio extends Item
{
    /** The audio media.*/
    public ConstrainedMediaDesc audioMedia;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.AUDIO;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (audioMedia != null) && audioMedia.isAudio() &&
            nonBlank(name, MAX_NAME_LENGTH);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return audioMedia;
    }

    @Override // from Item
    public ConstrainedMediaDesc getPrimaryMedia ()
    {
        return audioMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (ConstrainedMediaDesc desc)
    {
        audioMedia = desc;
    }
}
