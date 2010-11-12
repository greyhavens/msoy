//
// $Id: $
package com.threerings.msoy.server;

import com.samskivert.util.Calendars;

import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.web.server.CloudfrontException;
import com.threerings.msoy.web.server.CloudfrontURLSigner;
import com.threerings.orth.data.MediaDesc;

/**
 * A central class for statically constructor media descriptors that are ready to view
 * on a client, which for us means they need to be signed for Amazon's CloudFront service.
 */
public abstract class MediaDescFactory
{
    // let's let references live for a week to begin with
    public static int EXPIRATION_DAYS = 7;

    /** Return a signed media with the given hash and media type, but no constraint. */
    public static CloudfrontMediaDesc createMediaDesc (byte[] hash, byte mimeType)
    {
        return createMediaDesc(hash, mimeType, MediaDesc.NOT_CONSTRAINED);
    }

    /** Sign a HashMediaDesc with the default expiration of 7 days. */
    public static CloudfrontMediaDesc createMediaDesc (HashMediaDesc desc)
    {
        return createMediaDesc(desc.hash, desc.getMimeType(), desc.getConstraint());
    }

    /** Sign a HashMediaDesc with the given expiration epoch. */
    public static CloudfrontMediaDesc createMediaDesc (HashMediaDesc desc, int expiration)
    {
        return createMediaDesc(desc.hash, desc.getMimeType(), desc.getConstraint());
    }

    /** Return a signed media descriptor with the given configuration and default expiration. */
    public static CloudfrontMediaDesc createMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        return createMediaDesc(hash, mimeType, constraint,
            (int) (Calendars.now().addDays(EXPIRATION_DAYS).toTime()/1000));
    }

    /** Return a signed media descriptor with the given configuration and expiration epoch. */
    public static CloudfrontMediaDesc createMediaDesc (
        byte[] hash, byte mimeType, byte constraint, int expiration)
    {
        try {
            String signature = CloudfrontURLSigner.encodeSignature(
                _signer.createSignature(HashMediaDesc.getMediaPath(hash, mimeType), expiration));
            return new CloudfrontMediaDesc(hash, mimeType, constraint, expiration, signature);

        } catch (CloudfrontException cfe) {
            throw new RuntimeException("Failed to sign media URL", cfe);
        }
    }

    /** Convenience method, to be phased out. */
    public static CloudfrontMediaDesc createMediaDesc (String s, byte mimeType, byte constraint)
    {
        return createMediaDesc(HashMediaDesc.stringToHash(s), mimeType, constraint);
    }

    /** Nasty convenience method, to be phased out. */
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
