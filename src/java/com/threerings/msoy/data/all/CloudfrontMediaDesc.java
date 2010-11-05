//
// $Id: $


package com.threerings.msoy.data.all;

import com.threerings.msoy.server.ServerConfig;

import org.apache.commons.codec.binary.Base64;

/**
 *
 */
public class CloudfrontMediaDesc extends HashMediaDesc
{
    public CloudfrontMediaDesc (
        byte[] hash, byte mimeType, byte constraint, int expiration, byte[] signature)
    {
        super(hash, mimeType, constraint);
        _expiration = expiration;
        _signature = signature;
    }

    @Override public String getMediaPath ()
    {
        if (_url == null) {
            _url = super.getMediaPath() + "?Expires=" + _expiration + "&Key-Pair-Id=" +
            ServerConfig.cloudSigningKeyId + "&Signature=" + signature();
        }
        return _url;
    }

    protected String signature ()
    {
        return new String(Base64.encodeBase64(_signature))
            .replace("+", "-").replace("=", "_").replace("/", "~");
    }

    protected final int _expiration;
    protected final byte[] _signature;

    /** Lazily constructed if/when needed. */
    protected String _url;
}
