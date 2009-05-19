//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the runtime data for a Launcher item.
 */
public class Launcher extends GameItem
{
    /** Indicates whether the game we're launching is an AVRG. */
    public boolean isAVRG;

    @Override // from Item
    public byte getType ()
    {
        return LAUNCHER;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }
}
