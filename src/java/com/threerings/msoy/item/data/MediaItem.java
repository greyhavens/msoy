//
// $Id$

package com.threerings.msoy.item.data;

/**
 * The base class for all digital items that have associated static media.
 */
public abstract class MediaItem extends Item
{
    /** The MIME type for plain UTF-8 text. */
    public static final byte TEXT_PLAIN = 0;

    /** The MIME type for PNG image data. */
    public static final byte IMAGE_PNG = 10;

    /** The MIME type for JPEG image data. */
    public static final byte IMAGE_JPEG = 11;

    /** The MIME type for GIF image data. */
    public static final byte IMAGE_GIF = 12;

    /** The MIME type for MPEG audio data. */
    public static final byte AUDIO_MPEG = 20;

    /** The MIME type for WAV audio data. */
    public static final byte AUDIO_WAV = 21;

    /** The MIME type for FLV video data. */
    public static final byte VIDEO_FLASH = 30;

    /** The MIME type for MPEG video data. */
    public static final byte VIDEO_MPEG = 31;

    /** The MIME type for Quicktime video data. */
    public static final byte VIDEO_QUICKTIME = 32;

    /** The MIME type for AVI video data. */
    public static final byte VIDEO_MSVIDEO = 33;

    /** The MIME type for Flash SWF files. */
    public static final byte APPLICATION_SHOCKWAVE_FLASH = 40;

    /** A hash code identifying the media associated with this item. */
    public String mediaHash;

    /** The MIME type of the media associated with this item. */
    public byte mimeType;
}
