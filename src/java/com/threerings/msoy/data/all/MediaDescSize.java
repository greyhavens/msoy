//
// $Id: $

package com.threerings.msoy.data.all;

public abstract class MediaDescSize
{

    /** Identifies that a "quarter thumbnail" sized image is desired. */
    public static final int QUARTER_THUMBNAIL_SIZE = 0;
    /** Identifies that a "half thumbnail" sized image is desired. */
    public static final int HALF_THUMBNAIL_SIZE = 1;
    /** Identifies that a thumbnail sized image is desired. */
    public static final int THUMBNAIL_SIZE = 2;
    /** Identifies that a preview sized image is desired. */
    public static final int PREVIEW_SIZE = 3;
    /** The "thumbnail" size for scene snapshots. */
    public static final int SNAPSHOT_THUMB_SIZE = 4;
    /** The full size for canonical scene snapshots. */
    public static final int SNAPSHOT_FULL_SIZE = 5;
    /** The full size for game screenshots. */
    public static final int GAME_SHOT_SIZE = 6;
    /** The smallest size of room snapshots. */
    public static final int SNAPSHOT_TINY_SIZE = 7;
    /** Identifies the game splash logo size. */
    public static final int GAME_SPLASH_SIZE = 8;
    /** The full size for facbeook feed images. */
    public static final int FB_FEED_SIZE = 9;
    /** The full size for the Whirled logo (and themed replacements). */
    public static final int LOGO_SIZE = 10;
    /** The full size for a Whirled (tab) navigation button (and themed replacements). */
    public static final int NAV_SIZE = 11;
    /** The thumbnail image width.  */
    public static final int THUMBNAIL_WIDTH = 80;
    /** The thumbnail image height.  */
    public static final int THUMBNAIL_HEIGHT = 60;
    /**
     * @return the pixel width of any MediaDesc that's displayed at the given size.
     */
    public static int getWidth (int size)
    {
        return MediaDescSize.DIMENSIONS[2 * size];
    }
    /**
     * @return the pixel height of any MediaDesc that's displayed at the given size.
     */
    public static int getHeight (int size)
    {
        return MediaDescSize.DIMENSIONS[(2 * size) + 1];
    }

    /** Defines the dimensions of our various image sizes. */
    protected static final int[] DIMENSIONS = {
        THUMBNAIL_WIDTH/4, THUMBNAIL_HEIGHT/4, // quarter thumbnail size
        THUMBNAIL_WIDTH/2, THUMBNAIL_HEIGHT/2, // half thumbnail size
        THUMBNAIL_WIDTH,   THUMBNAIL_HEIGHT,   // thumbnail size
        THUMBNAIL_WIDTH*4, THUMBNAIL_HEIGHT*4, // preview size
        175, 100, // scene snapshot thumb size
        350, 200, // full scene snapshot image size
        175, 125, // game screenshots
         40,  23, // tiny snapshots, same width as half thumbnail
        700, 500, // game splash image, same as the game window
        130, 130, // facebook feed thumbnail
        300, 50,  // whirled logo size
        76, 32,   // navigation (tab) button
    };

}
