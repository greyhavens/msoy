//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * The base class for all digital items that have associated static media.
 */
public /*abstract*/ class MediaItem extends Item
{
    /** The MIME type for plain UTF-8 text. */
    public static const TEXT_PLAIN :int = 0;

    /** The MIME type for PNG image data. */
    public static const IMAGE_PNG :int = 10;

    /** The MIME type for JPEG image data. */
    public static const IMAGE_JPEG :int = 11;

    /** The MIME type for GIF image data. */
    public static const IMAGE_GIF :int = 12;

    /** The MIME type for MPEG audio data. */
    public static const AUDIO_MPEG :int = 20;

    /** The MIME type for WAV audio data. */
    public static const AUDIO_WAV :int = 21;

    /** The MIME type for FLV video data. */
    public static const VIDEO_FLASH :int = 30;

    /** The MIME type for MPEG video data. */
    public static const VIDEO_MPEG :int = 31;

    /** The MIME type for Quicktime video data. */
    public static const VIDEO_QUICKTIME :int = 32;

    /** The MIME type for AVI video data. */
    public static const VIDEO_MSVIDEO :int = 33;

    /** The MIME type for Flash SWF files. */
    public static const APPLICATION_SHOCKWAVE_FLASH :int = 40;

    /** A hash code identifying the media associated with this item. */
    public var mediaHash :ByteArray;

    /** The MIME type of the media associated with this item. */
    public var mimeType :int;

    /**
     * Returns the path of the URL that references this media.
     */
    public function getMediaPath () :String
    {
        return MediaItem.getMediaPath(mediaHash, mimeType);
    }

    /**
     * Get the path of the URL for the media specified.
     */
    public static function getMediaPath (
        mediaHash :ByteArray, mimeType :int) :String
    {
        return "/media/" + hashToString(mediaHash) +
            mimeTypeToSuffix(mimeType);
    }

    /**
     * Convert the specified media hash into a String
     */
    public static function hashToString (hash :ByteArray) :String
    {
        return StringUtil.hexlate(hash);
    }

    /**
     * Maps the supplied string representation of a mime type to our internal
     * integer code. Returns -1 if the mime type is unknown.
     */
    public static function stringToMimeType (mimeType :String) :int
    {
        mimeType = mimeType.toLowerCase();
        if (mimeType == "text/plain") {
            return TEXT_PLAIN;
        } else if (mimeType == "image/png") {
            return IMAGE_PNG;
        } else if (mimeType == "image/jpeg") {
            return IMAGE_JPEG;
        } else if (mimeType == "image/gif") {
            return IMAGE_GIF;
        } else if (mimeType == "audio/mpeg") {
            return AUDIO_MPEG;
        } else if (mimeType == "audio/wav") {
            return AUDIO_WAV;
        } else if (mimeType == "video/flash") {
            return VIDEO_FLASH;
        } else if (mimeType == "video/mpeg") {
            return VIDEO_MPEG;
        } else if (mimeType == "video/quicktime") {
            return VIDEO_QUICKTIME;
        } else if (mimeType == "video/msvideo") {
            return VIDEO_MSVIDEO;
        } else if (mimeType == "application/x-shockwave-flash") {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else {
            return -1;
        }
    }

    /**
     * Maps the supplied filename suffix to a mime type. Returns -1 if the
     * suffix is unknown.
     */
    public static function suffixToMimeType (filename :String) :int
    {
        filename = filename.toLowerCase();
        if (StringUtil.endsWith(filename, ".txt")) {
            return TEXT_PLAIN;
        } else if (StringUtil.endsWith(filename, ".png")) {
            return IMAGE_PNG;
        } else if (StringUtil.endsWith(filename, ".jpg")) {
            return IMAGE_JPEG;
        } else if (StringUtil.endsWith(filename, ".gif")) {
            return IMAGE_GIF;
        } else if (StringUtil.endsWith(filename, ".mp3")) {
            return AUDIO_MPEG;
        } else if (StringUtil.endsWith(filename, ".wav")) {
            return AUDIO_WAV;
        } else if (StringUtil.endsWith(filename, ".flv")) {
            return VIDEO_FLASH;
        } else if (StringUtil.endsWith(filename, ".mpg")) {
            return VIDEO_MPEG;
        } else if (StringUtil.endsWith(filename, ".mov")) {
            return VIDEO_QUICKTIME;
        } else if (StringUtil.endsWith(filename, ".avi")) {
            return VIDEO_MSVIDEO;
        } else if (StringUtil.endsWith(filename, ".swf")) {
            return APPLICATION_SHOCKWAVE_FLASH;
        } else {
            return -1;
        }
    }

    /**
     * Returns a file suffix for use with the specified mime tpye or .dat if
     * mime type is unknown.
     */
    public static function mimeTypeToSuffix (mimeType :int) :String
    {
        switch (mimeType) {
        case TEXT_PLAIN: return ".txt";
        case IMAGE_PNG: return ".png";
        case IMAGE_JPEG: return ".jpg";
        case IMAGE_GIF: return ".gif";
        case AUDIO_MPEG: return ".mp3";
        case AUDIO_WAV: return ".wav";
        case VIDEO_FLASH: return ".flv";
        case VIDEO_MPEG: return ".mpg";
        case VIDEO_QUICKTIME: return ".mov";
        case VIDEO_MSVIDEO: return ".avi";
        case APPLICATION_SHOCKWAVE_FLASH: return ".swf";
        default: return ".dat";
        }
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(mediaHash);
        out.writeByte(mimeType);
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        mediaHash = (ins.readField(ByteArray) as ByteArray);
        mimeType = ins.readByte();
    }
}
}
