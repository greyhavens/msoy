//
// $Id: MediaDesc.as 18842 2009-12-11 20:38:56Z zell $

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.util.Hashable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A class containing metadata about a media object.
 */
public class MediaDescSize
{
    /** Identifies that a "quarter thumbnail" sized image is desired. */
    public static const QUARTER_THUMBNAIL_SIZE :int = 0;

    /** Identifies that a "half thumbnail" sized image is desired. */
    public static const HALF_THUMBNAIL_SIZE :int = 1;

    /** Identifies that a thumbnail sized image is desired. */
    public static const THUMBNAIL_SIZE :int = 2;

    /** Identifies that a preview sized image is desired. */
    public static const PREVIEW_SIZE :int = 3;

    /** The "thumbnail" size for scene snapshots. */
    public static const SNAPSHOT_THUMB_SIZE :int = 4;

    /** The full size for canonical scene snapshots. */
    public static const SNAPSHOT_FULL_SIZE :int = 5;

    /** The full size for game screenshots. */
    public static const GAME_SHOT_SIZE :int = 6;

    /** The smallest size of room snapshots. */
    public static const SNAPSHOT_TINY_SIZE :int = 7;

    /** Identifies the game splash logo size. */
    public static const GAME_SPLASH_SIZE :int = 8;

    /** The full size for facebook feed images. */
    public static const FB_FEED_SIZE :int = 9;

    /** The thumbnail image width.  */
    public static const THUMBNAIL_WIDTH :int = 80;

    /** The thumbnail image height.  */
    public static const THUMBNAIL_HEIGHT :int = 60;

    /** Defines the dimensions of our various image sizes. */
    public static const DIMENSIONS :Array = [
        THUMBNAIL_WIDTH/4, THUMBNAIL_HEIGHT/4, // quarter thumbnail size
        THUMBNAIL_WIDTH/2, THUMBNAIL_HEIGHT/2, // half thumbnail size
        THUMBNAIL_WIDTH,   THUMBNAIL_HEIGHT,   // thumbnail size
        THUMBNAIL_WIDTH*4, THUMBNAIL_HEIGHT*4, // preview size
        175, 100, // scene snapshot thumb size
        350, 200, // full scene snapshot image size
        175, 125, // game screenshots
         40,  23, // tiny snapshots, same width as half thumbnail
        700, 500, // game splash image, same as the min game window
        130, 130, // facebook feed thumbnail
    ];

    /**
     * Gets the pixel width associated with the given size.
     */
    public static function getWidth (size :int) :int
    {
        return DIMENSIONS[size * 2];
    }

    /**
     * Gets the pixel height associated with the given size.
     */
    public static function getHeight (size :int) :int
    {
        return DIMENSIONS[size * 2 + 1];
    }
}
}
