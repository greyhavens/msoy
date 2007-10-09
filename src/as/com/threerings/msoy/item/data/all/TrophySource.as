//
// $Id$

package com.threerings.msoy.item.data.all {

/**
 * Contains the runtime data for a TrophySource item.
 */
public class TrophySource extends Item
{
    /** The required width for a trophy image. */
    public static const TROPHY_WIDTH :int = 60;

    /** The required height for a trophy image. */
    public static const TROPHY_HEIGHT :int = 60;

    public function TrophySource ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia();
    }

    // from Item
    override public function getType () :int
    {
        return TROPHY_SOURCE;
    }
}
}
