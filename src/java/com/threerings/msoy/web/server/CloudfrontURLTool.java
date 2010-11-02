//
// $Id: $

package com.threerings.msoy.web.server;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.codec.binary.Base64;

import static com.threerings.msoy.Log.log;

/**
 * A class dedicated to the creation of Signed URLs for CloudFront, as specified in
 *
 *     http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/
 */
public class CloudfrontURLTool
{
    /**
     * This class must be instantiated with the private half of a CloudFront signature key pair.
     */
    public CloudfrontURLTool (String signingKeyId, String signingPrivateKey)
    {
        this(signingKeyId, Base64.decodeBase64(signingPrivateKey.getBytes()));
    }

    /**
     * This class must be instantiated with the private half of a CloudFront signature key pair.
     */
    public CloudfrontURLTool (String signingKeyId, byte[] privateKey)
    {
        _signingKeyId = signingKeyId;
        _privateKey = privateKey;
    }

    /**
     * Sign the given URL that expires at the given epoch, and return the result. Currently we
     * assume the URL has no query parameters, i.e. no ?foo=bar bits.
     *
     * TODO: Remove restrictions, they are only motivated by laziness.
     */
    public void signURL (String urlString, int expirationEpoch)
        throws CloudfrontException
    {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new CloudfrontException("Bad URL.", e);
        }

        if (!url.getProtocol().equals("http")) {
            throw new CloudfrontException("Can only sign HTTP URLs.");
        }
        if (url.getQuery() != null) {
            throw new CloudfrontException("Can't sign URLs with query bits.");
        }

        // {"Statement":[{"Resource":"RESOURCE","Condition":{"DateLessThan":{"AWS:EpochTime":EXPIRES}}}]}
        String policy = "{\"Statement\":[{\"Resource\":\"" + urlString +
            "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + expirationEpoch + "}}}]}";
    }

    protected String _signingKeyId;
    protected byte[] _privateKey;
}