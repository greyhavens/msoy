//
// $Id: $


package com.threerings.msoy.server;

import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.web.server.CloudfrontException;
import com.threerings.msoy.web.server.CloudfrontURLSigner;
import com.threerings.orth.data.MediaDesc;

/**
 *
 */
public class MediaDescFactory
{
    // let's let references live for a week to begin with
    public static int EXPIRATION_SECONDS = 7 * 24 * 3600;

    public static CloudfrontMediaDesc createMediaDesc (HashMediaDesc desc)
    {
        return createMediaDesc(desc.hash, desc.getMimeType(), desc.getConstraint());
    }

    public static CloudfrontMediaDesc createMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        try {
            int now = ((int) (System.currentTimeMillis() / 1000));
            int expiration = now + EXPIRATION_SECONDS;
            String signature = CloudfrontURLSigner.encodeSignature(_signer.createSignature(
                HashMediaDesc.getMediaPath(hash, mimeType), expiration));

            return new CloudfrontMediaDesc(hash, mimeType, constraint, expiration, signature);

        } catch (CloudfrontException cfe) {
            throw new RuntimeException("Failed to sign media URL", cfe);
        }
    }

    public static CloudfrontMediaDesc createMediaDesc (byte[] hash, byte mimeType)
    {
        return createMediaDesc(hash, mimeType, MediaDesc.NOT_CONSTRAINED);
    }

    public static CloudfrontMediaDesc createMediaDesc (String s, byte mimeType, byte constraint)
    {
        return createMediaDesc(HashMediaDesc.stringToHash(s), mimeType, constraint);
    }

    public static CloudfrontMediaDesc createMediaDesc (String filename)
    {
        return createMediaDesc(
            HashMediaDesc.stringToHash(filename.substring(0, filename.indexOf('.'))),
            MediaMimeTypes.suffixToMimeType(filename),
            MediaDesc.NOT_CONSTRAINED);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, MediaDesc onNull)
    {
        return (hash == null) ? onNull : createMediaDesc(hash, mimeType);
    }

    /**
     * Creates and returns a media descriptor if the supplied hash is non-null, returns onNull
     * otherwise.
     */
    public static MediaDesc make (byte[] hash, byte mimeType, byte constraint, MediaDesc onNull)
    {
        return (hash == null) ? onNull : createMediaDesc(hash, mimeType, constraint);
    }

    public static CloudfrontURLSigner _signer = new CloudfrontURLSigner(
        ServerConfig.cloudSigningKeyId, ServerConfig.cloudSigningKey);
}
