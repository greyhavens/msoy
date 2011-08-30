//
// $Id: $

package com.threerings.msoy.web.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * A class dedicated to the creation of Signed URLs for CloudFront, as specified in
 *
 *     http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/
 */
public class CloudfrontURLSigner
{
    public static String encodeSignature (byte[] signature)
    {
        return new String(Base64.encodeBase64(signature))
            .replace("+", "-").replace("=", "_").replace("/", "~");
    }

    /**
     * This class must be instantiated with the private half of a CloudFront signature key pair.
     */
    public CloudfrontURLSigner (String signingKeyId, String signingKey)
    {
        this(signingKeyId, Base64.decodeBase64(signingKey.getBytes()));
    }

    /**
     * This class must be instantiated with the private half of a CloudFront signature key pair.
     */
    public CloudfrontURLSigner (String signingKeyId, byte[] signingKeyBytes)
    {
        _signingKeyId = signingKeyId;
        _signingKeyBytes = signingKeyBytes;
    }

    /**
     * Sign the given URL that expires at the given epoch, and return the result. Currently we
     * assume the URL has no query parameters, i.e. no ?foo=bar bits.
     *
     * TODO: Remove restrictions, they are only motivated by laziness.
     */
    public String signURL (String nakedUrl, int expiration)
        throws CloudfrontException
    {
        URL url;
        try {
            url = new URL(nakedUrl);
        } catch (MalformedURLException e) {
            throw new CloudfrontException("Bad URL.", e);
        }

        if (!url.getProtocol().equals("http")) {
            throw new CloudfrontException("Can only sign HTTP URLs.");
        }
        if (url.getQuery() != null) {
            throw new CloudfrontException("Can't sign URLs with query bits.");
        }

        return nakedUrl + "?Expires=" + expiration + "&Key-Pair-Id=" +
            _signingKeyId + "&Signature=" + encodeSignature(createSignature(nakedUrl, expiration));
    }

    public byte[] createSignature (String nakedUrl, int expirationEpoch)
        throws CloudfrontException
    {
        // {"Statement":[{"Resource":"RSRC","Condition":{"DateLessThan":{"AWS:EpochTime":EXPR}}}]}
        String policy = "{\"Statement\":[{\"Resource\":\"" + nakedUrl +
            "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + expirationEpoch + "}}}]}";
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            sig.initSign(keyFactory.generatePrivate(new PKCS8EncodedKeySpec(_signingKeyBytes)));
            sig.update(policy.getBytes());
            return sig.sign();

        } catch (GeneralSecurityException e) {
            throw new CloudfrontException("Cryptographic failure signing URL", e);
        }
    }

    protected String _signingKeyId;
    protected byte[] _signingKeyBytes;
}
