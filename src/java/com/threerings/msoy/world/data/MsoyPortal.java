//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.MediaData;

/**
 * Contains additional info about portals necessary for the msoy world.
 */
public class MsoyPortal extends Portal
{
    /** The media used to represent the portal. */
    public MediaData media;

    /** The x scale factor for the media. */
    public float scaleX = 1;

    /** The y scale factor for the media. */
    public float scaleY = 1;
}
