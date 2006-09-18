package com.threerings.msoy.item.web {

import flash.geom.Point;

import flash.utils.ByteArray;

import com.threerings.util.Hashable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * A class containing metadata about a media object.
 */
public class MediaDesc
    implements Hashable, Streamable
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

    /** A hash code identifying this media. */
    public var hash :ByteArray;

    /** The MIME type of this media. */
    public var mimeType :int;

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

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function MediaDesc (hash :ByteArray = null, mimeType :int = 0)
    {
        this.hash = hash;
        this.mimeType = mimeType;
    }

    /**
     * Returns the path of the URL that references this media.
     */
    public function getMediaPath () :String
    {
        return "/media/" + hashToString(hash) +
            mimeTypeToSuffix(mimeType);
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        return getMediaPath();
    }

    /**
     * @return true if the media is clickable.
     */
    public function isInteractive () :Boolean
    {
        // TODO: this may need to be more complicated in the future
        switch (mimeType) {
        case APPLICATION_SHOCKWAVE_FLASH:
            return true;

        default:
            return false;
        }
    }

    // documentation inherited from Hashable
    public function hashCode () :int
    {
        var code :int = 0;
        for (var ii :int = Math.min(3, hash.length - 1); ii >= 0; ii--) {
            code = (code << 8) | hash[ii];
        }
        return code;
    }

    // documentation inherited from Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is MediaDesc) {
            var that :MediaDesc = (other as MediaDesc);
            return (this.mimeType == that.mimeType) &&
                Util.equals(this.hash, that.hash);
        }
        return false;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(hash);
        out.writeByte(mimeType);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        hash = (ins.readField(ByteArray) as ByteArray);
        mimeType = ins.readByte();
    }

//    /* TEMP. */
//    protected static const DATA :Array = [
//        // [ mediaURI, isInteractive, hotspot ]
// /* 0*/ [ "hipsterzombie.swf", true, new Point(126, 367) ],
//        [ "socketbunny.swf", true, new Point(155, 360) ],
//        //[ "TestAvatar.swf", false ],
//        [ "pedestrian.swf", true ],
//        [ "alleydoor.swf", true, new Point(36, 355) ],
//        [ "bigvid.flv", false ],
//        //[ "flv/320x240.swf?url=JoshuaTree.flv", false ],
// //* 5*/ [ "JoshuaTree.flv", false ],
// /* 5*/ [ "mario.flv", false ],
//        [ "rainbowdoor.swf", true, new Point(144, 367) ],
//        [ "fans.swf", false ],
//        [ "fancyroom.png", false ],
//        [ "pinball.swf", true, new Point(100, 252) ],
// /*10*/ [ "directorschair.swf", false, new Point(54, 154) ],
//        [ "alley.png", false ],
//        [ "curtain.swf", true, new Point(130, 530) ],
//        [ "BollWeevil.mp3", false ],
//        [ "http://sd165.sivit.org/random_4432/files/420.swf",
//                false, null, true ],
//// /*15*/ [ "http://www.microgames.info/games/3d_logic/loader_3d_logic.swf",
////                true, null, true ],
// /*15*/ [ "Reversi.swf", true ],
//        [ "comicroom.png", false ],
//        [ "comicroomforeground.png", false ],
//        [ "bendaydoor.swf", false ],
//        [ "bendaytransport.swf", false ],
// /*20*/ [ "teenqueen.swf", true ],
//        [ "critter.swf", true ],
//        [ "wendle6.swf", true, new Point(116, 362) ],
//        //[ "cliff_background.png", false ],
//        [ "cliff_background_anim.swf", false ],
//        [ "cliff_foreground.png", false ],
// /*25*/ [ "octocar_all.swf", false ],
//        [ "faucet_scene.png", false ],
//        [ "pipe_door.png", false, new Point(40, 215) ],
//        [ "crayon_room.png", false ],
//        [ "cactus.png", false ],
// /*30*/ [ "fishbowl.png", false ],
//        [ "ladder.png", false, new Point(70, 36) ],
//        [ "red_door.png", false, new Point(90, 333) ],
//        [ "aqua_door.png", false ],
//        [ "smile_door.png", false, new Point(88, 327) ],
// /*35*/ [ "candles.png", false, new Point(76, 0) ],
//        [ "cedric.swf", false, new Point(133, 376) ],
//        [ "frame.png", false, new Point(151, 111) ],
//        [ "soy.swf", false ],
//        [ "mthomasbunny.swf", false, new Point(155, 360) ],
// /*40*/ [ "mjohnsonbunny.swf", false, new Point(155, 360) ],
//        [ "npopovichbunny.swf", false, new Point(155, 360) ],
//        [ "rbeerbunny.swf", false, new Point(155, 360) ],
//        [ "akapolkabunny.swf", false, new Point(155, 360) ],
//        [ "mbaynebunny.swf", false, new Point(155, 360) ],
// /*45*/ [ "jleplastrierbunny.swf", false, new Point(155, 360) ],
//        [ "jgramsebunny.swf", false, new Point(155, 360) ],
//        [ "template_human.swf", false, new Point(155, 360) ],
//        [ "rgreenwellbunny.swf", false, new Point(155, 360) ],
//        [ "rbeerbunny2.swf", false, new Point(155, 360) ],
// /*50*/ [ "mtungbunny.swf", false, new Point(155, 360) ],
//        [ "clothestest.swf", false ],
//        [ "humanbase.swf", false ],
//        [ "square100.png", false, new Point(50, 50) ],
//        [ "djamesbunny.swf", false, new Point(155, 360) ],
// /*55*/ [ "ClickFest.swf", true ],
//        [ "BigTwo.swf", true ],
//    ];
}
}
