//
// $Id: $

package com.threerings.msoy.data.all {

/**
 * A hash-based MediaDesc that has been signed for Cloudfront access. We do not do our
 * own Base64-encoding on the client, mostly because GWT is stupid about turning byte[]
 * into String, and we get our signing key from DeploymentConfig so we don't have to
 * send it down the wire pointlessly thousands of times.
 */
public class CloudfrontMediaDesc extends HashMediaDesc
{
    public function CloudfrontMediaDesc (
        hash :ByteArray = null, mimeType :int, constraint :int, expiration :int = 0,
        signature :String = null)
    {
        super(hash, mimeType, constraint);
        _expiration = expiration;
        _signature = signature;
    }

    override public String getMediaPath ()
    {
        if (_url == null) {
            _url = super.getMediaPath() + "?Expires=" + _expiration + "&Key-Pair-Id=" +
                DeploymentConfig.signingKeyId + "&Signature=" + _signature;
        }
        return _url;
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

    protected final var _expiration :int;
    protected final var _signature :String;

    /** Lazily constructed if/when needed. */
    protected var _url :String;
}
