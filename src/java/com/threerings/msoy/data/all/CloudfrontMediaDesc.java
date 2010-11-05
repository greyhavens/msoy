//
// $Id: $


package com.threerings.msoy.data.all;

/**
 * A hash-based MediaDesc that has been signed for Cloudfront access. We do not do our
 * own Base64-encoding on the client, mostly because GWT is stupid about turning byte[]
 * into String, and we get our signing key from DeploymentConfig so we don't have to
 * send it down the wire pointlessly thousands of times.
 */
public class CloudfrontMediaDesc extends HashMediaDesc
{
    public CloudfrontMediaDesc (
        byte[] hash, byte mimeType, byte constraint, int expiration, String signature)
    {
        super(hash, mimeType, constraint);
        _expiration = expiration;
        _signature = signature;
    }

    @Override public String getMediaPath ()
    {
        if (_url == null) {
            _url = super.getMediaPath() + "?Expires=" + _expiration + "&Key-Pair-Id=" +
                DeploymentConfig.signingKeyId + "&Signature=" + _signature;
        }
        return _url;
    }

    protected final int _expiration;
    protected final String _signature;

    /** Lazily constructed if/when needed. */
    protected String _url;
}
