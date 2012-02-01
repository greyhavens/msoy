//
// $Id: $

package com.threerings.msoy.data.all {

import flash.utils.ByteArray;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.client.DeploymentConfig;

/**
 * A hash-based MediaDesc that has been signed for Cloudfront access. We do not do our
 * own Base64-encoding on the client, mostly because GWT is stupid about turning byte[]
 * into String, and we get our signing key from DeploymentConfig so we don't have to
 * send it down the wire pointlessly thousands of times.
 */
public class CloudfrontMediaDesc extends HashMediaDesc
{
    public function CloudfrontMediaDesc (
        hash :ByteArray = null, mimeType :int = 0, constraint :int = NOT_CONSTRAINED,
        expiration :int = 0, signature :String = null)
    {
        super(hash, mimeType, constraint);
        _expiration = expiration;
        _signature = signature;
    }

    override public function getMediaPath () :String
    {
        if (_url == null) {
            // NOTE: Signed URLs are disabled, see commit log for r20143
            // _url = super.getMediaPath() + "?Expires=" + _expiration + "&Key-Pair-Id=" +
            //     DeploymentConfig.signingKeyId + "&Signature=" + _signature;
            _url = super.getMediaPath();
        }
        return _url;
    }

    // from Object
    override public function toString () :String
    {
        return hashToString(hash) + ":" + mimeType + ":" + _expiration + " (signed)";
    }

    // documentation inherited from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _expiration = ins.readInt();
        _signature = (ins.readField(String) as String);
    }

    // documentation inherited from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(_expiration);
        out.writeField(_signature);
    }

    protected var _expiration :int;
    protected var _signature :String;

    /** Lazily constructed if/when needed. */
    protected var _url :String;
}
}
