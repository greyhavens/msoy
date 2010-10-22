//
// $Id: MediaDesc.as 18283 2009-10-06 20:57:49Z jamie $

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

import com.threerings.util.Hashable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A class containing metadata about a media object.
 */
public class HashMediaDesc extends MediaDescImpl
{
    /** The SHA-1 hash of this media's data. */
    public var hash :ByteArray;

    /**
     * Convert the specified media hash into a String
     */
    public static function hashToString (hash :ByteArray) :String
    {
        return StringUtil.hexlate(hash);
    }

    /**
     * Convert the specified String back into a media hash.
     */
    public static function stringToHash (hash :String) :ByteArray
    {
        return StringUtil.unhexlate(hash);
    }

    /**
     * Creates a colon-delimeted String from a MediaDesc.
     */
    public static function mdToString (md :MediaDesc) :String
    {
        if (md is HashMediaDesc) {
            return hashToString((md as HashMediaDesc).hash) + ":" + md.mimeType + ":" + md.constraint;
        }
        return "";
    }

    /**
     * Creates a MediaDesc from a colon-delimited String.
     */
    public static function stringToMD (str :String) :MediaDesc
    {
        var data :Array = str.split(":");
        if (data.length != 3) {
            return null;
        }

        var hash :ByteArray = stringToHash(data[0]);
        if (hash == null) {
            return null;
        }
        var mimeType :int = parseInt(data[1]);
        var constraint :int = parseInt(data[2]);
        return new HashMediaDesc(hash, mimeType, constraint);
    }

    /**
     * Creates either a configured or blank media descriptor.
     */
    public function HashMediaDesc(
        hash :ByteArray = null, mimeType :int = 0, constraint :int = NOT_CONSTRAINED)
    {
        super(mimeType, constraint);

        this.hash = hash;
    }

    // from EntityMedia
    override public function getMediaPath () :String
    {
        if (hash == null) {
            return null;
        }
        return DeploymentConfig.mediaURL + hashToString(hash) +
            MediaMimeTypes.mimeTypeToSuffix(mimeType);
    }

    /**
     * Is this media bleepable?
     */
    override public function isBleepable () :Boolean
    {
        return (hash != null);
    }

    // from MediaDesc
    override public function getMediaId () :String
    {
        return hashToString(hash);
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
    override public function equals (other :Object) :Boolean
    {
        return (other is HashMediaDesc) && super.equals(other)
            && Util.equals(this.hash, (other as HashMediaDesc).hash);
    }

    // from Object
    public function toString () :String
    {
        return hashToString(hash) + ":" + mimeType;
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        hash = (ins.readField(ByteArray) as ByteArray);
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeField(hash);
    }
}
}
