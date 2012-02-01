//
// $Id: $

package com.threerings.msoy.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * A hash-based MediaDesc that has been signed for Cloudfront access. We do not do our
 * own Base64-encoding on the client, mostly because GWT is stupid about turning byte[]
 * into String, and we get our signing key from DeploymentConfig so we don't have to
 * send it down the wire pointlessly thousands of times.
 */
public class CloudfrontMediaDesc extends HashMediaDesc
{
    /**
     * Creates a MediaDesc from a colon delimited String.
     */
    public static CloudfrontMediaDesc stringToCFMD (String str)
    {
        String[] data = str.split(":");
        if (data.length != 5) {
            return null;
        }
        byte[] hash = HashMediaDesc.stringToHash(data[0]);
        if (hash == null) {
            return null;
        }
        byte mimeType = MediaMimeTypes.INVALID_MIME_TYPE;
        byte constraint = 0;
        try {
            mimeType = Byte.parseByte(data[1]);
        } catch (NumberFormatException nfe) {
            // don't care
        }
        try {
            constraint = Byte.parseByte(data[2]);
        } catch (NumberFormatException nfe) {
            // don't care
        }

        int expiration;
        try {
            expiration = Integer.parseInt(data[3]);
        } catch (NumberFormatException nfe) {
            // this is actually quite bad
            return null;
        }

        String signature = data[4];
        return new CloudfrontMediaDesc(hash, mimeType, constraint, expiration, signature);
    }

    @Override public MediaDesc newWithConstraint (byte constraint)
    {
        return new CloudfrontMediaDesc(this.hash, _mimeType, constraint, _expiration, _signature);
    }

    /**
     * Converts a MediaDesc into a colon delimited String.
     */
    public static String cfmdToString (CloudfrontMediaDesc cfmd)
    {
        return new StringBuilder(hashToString(cfmd.hash))
            .append(":").append(cfmd.getMimeType())
            .append(":").append(cfmd.getConstraint())
            .append(":").append(cfmd.getExpiration())
            .append(":").append(cfmd.getSignature())
            .toString();
    }

    /** For serialization. */
    public CloudfrontMediaDesc ()
	{
	}

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
            // NOTE: Signed URLs are disabled, see commit log for r20143
            // _url = super.getMediaPath() + "?Expires=" + _expiration + "&Key-Pair-Id=" +
            //     DeploymentConfig.signingKeyId + "&Signature=" + _signature;
            _url = super.getMediaPath();
        }
        return _url;
    }

    public int getExpiration ()
    {
        return _expiration;
    }

    public String getSignature ()
    {
        return _signature;
    }

    @Override public String toString ()
    {
        return super.toString() + "/" + _expiration + " (signed)";
    }

    protected int _expiration;
    protected String _signature;

    /** Lazily constructed if/when needed. */
    protected transient String _url;
}
