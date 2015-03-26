//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.util.Calendars;

import com.threerings.orth.data.MediaDesc;

// import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.server.util.JSONMarshaller.JSONMutator;
import com.threerings.msoy.server.util.JSONMarshaller;
// import com.threerings.msoy.web.server.CloudfrontException;
// import com.threerings.msoy.web.server.CloudfrontURLSigner;

import static com.threerings.msoy.Log.log;

/**
 * A central class for statically constructor media descriptors that are ready to view on a client.
 *
 * This used to mean they needed to be signed for Amazon's CloudFront service, but we disabled that
 * when we went rogue.
 */
public abstract class MediaDescFactory
{
    // let's let references live for a week to begin with
    public static int EXPIRATION_DAYS = 7;

    /** Return a signed media with the given hash and media type, but no constraint. */
    public static HashMediaDesc createMediaDesc (byte[] hash, byte mimeType)
    {
        return createMediaDesc(hash, mimeType, MediaDesc.NOT_CONSTRAINED);
    }

    /** Sign a HashMediaDesc with the default expiration of 7 days. */
    public static HashMediaDesc createMediaDesc (HashMediaDesc desc)
    {
        return createMediaDesc(desc.hash, desc.getMimeType(), desc.getConstraint());
    }

    /** Sign a HashMediaDesc with the given expiration epoch. */
    public static HashMediaDesc createMediaDesc (HashMediaDesc desc, int expiration)
    {
        return createMediaDesc(desc.hash, desc.getMimeType(), desc.getConstraint());
    }

    /** Return a signed media descriptor with the given configuration and default expiration. */
    public static HashMediaDesc createMediaDesc (byte[] hash, byte mimeType, byte constraint)
    {
        return createMediaDesc(hash, mimeType, constraint,
            (int) (Calendars.now().addDays(EXPIRATION_DAYS).toTime()/1000));
    }

    /** Return a signed media descriptor with the given configuration and expiration epoch. */
    public static HashMediaDesc createMediaDesc (
        byte[] hash, byte mimeType, byte constraint, int expiration)
    {
        // String signature;
        // try {
        //     signature = CloudfrontURLSigner.encodeSignature(
        //         _signer.createSignature(HashMediaDesc.getMediaPath(hash, mimeType), expiration));
        // } catch (CloudfrontException cfe) {
        //     log.warning("Failed to sign media URL", cfe);
        //     signature = ""; // the media will fail to load, but the server will live on
        // }
        // return new CloudfrontMediaDesc(hash, mimeType, constraint, expiration, signature);
        return new HashMediaDesc(hash, mimeType, constraint);
    }

    /** Nasty convenience method, to be phased out. */
    public static HashMediaDesc createMediaDesc (String filename)
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

    // public static CloudfrontURLSigner _signer = new CloudfrontURLSigner(
    //     ServerConfig.cloudSigningKeyId, ServerConfig.cloudSigningKey);

    static {
        JSONMarshaller.registerMutator(HashMediaDesc.class, new JSONMutator<HashMediaDesc>() {
            public HashMediaDesc jsonMutate (HashMediaDesc obj) {
                return MediaDescFactory.createMediaDesc(obj);
            }
        });
    }
}
