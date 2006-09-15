//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains additional info about portals necessary for the msoy world.
 */
public class MsoyPortal extends Portal
{
    /** The media used to represent the portal. */
    public MediaDesc media;

    /** The x scale factor for the media. */
    public float scaleX = 1;

    /** The y scale factor for the media. */
    public float scaleY = 1;

    /**
     * @return true if the other portal is identical.
     */
    public boolean equivalent (MsoyPortal that)
    {
        return (this.portalId == that.portalId) &&
            this.loc.equals(that.loc) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY);
    }
}
