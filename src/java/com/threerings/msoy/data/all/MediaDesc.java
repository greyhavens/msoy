package com.threerings.msoy.data.all;

import com.threerings.orth.scene.data.EntityMedia;//
// $Id: $

public interface MediaDesc extends EntityMedia
{
    /**
     * Is this media merely an image type?
     */
    boolean isImage ();

    /**
     * Is this media a SWF?
     */
    boolean isSWF ();

    /**
     * Is this media purely audio?
     */
    boolean isAudio ();

    /**
     * Is this media video?
     */
    boolean isVideo ();

    boolean isExternal ();

    /**
     * Return true if this media has a visual component that can be shown in
     * flash.
     */
    boolean hasFlashVisual ();

    /**
     * Is this a zip of some sort?
     */
    boolean isRemixed ();

    /**
     * Is this media remixable?
     */
    boolean isRemixable ();
}
