//
// $Id$

package com.threerings.msoy.ui.skins {

public class VolumeButton
{
    /**
     * Get the image to use for the specified volume level.
     */
    public static function getImage (volume :Number) :Class
    {
        // if the level is 0, show image 0, else smoothly between 1 and 4
        const image :int = (volume == 0) ? 0 : (1 + Math.round(volume * 3));
        return IMAGES[image] as Class;
    }

    /** The images used to skin the volume button. */
    public static const IMAGES :Array = [
        VOLUME_0, VOLUME_1, VOLUME_2, VOLUME_3, VOLUME_4 ];

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/vol_0.png")]
    public static const VOLUME_0 :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/vol_1.png")]
    public static const VOLUME_1 :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/vol_2.png")]
    public static const VOLUME_2 :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/vol_3.png")]
    public static const VOLUME_3 :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/vol_4.png")]
    public static const VOLUME_4 :Class;
}
}
