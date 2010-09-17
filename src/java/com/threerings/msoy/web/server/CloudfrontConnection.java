//
// $Id: $

package com.threerings.msoy.web.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.megginson.sax.XMLWriter;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * An interface into the Cloudfront system. It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate Cloudfront distributions and object invalidation.
 *
 * TODO: Implement everything else.
 */
public class CloudfrontConnection
{
    public static class CloudfrontException extends Exception
    {
        public CloudfrontException (String message)
        {
            super(message);
        }

        public CloudfrontException (String message, Throwable cause)
        {
            super(message, cause);
        }
    }
    
    public CloudfrontConnection (String keyId, String secretKey)
    {
        this(keyId, secretKey, createDefaultHostConfig());
    }

    public CloudfrontConnection (
        String keyId, String secretKey, HostConfiguration hostConfig)
    {
        _keyId = keyId;
        _secretKey = secretKey;
        _httpClient = new HttpClient();
        _httpClient.setHostConfiguration(hostConfig);

        /* httpclient defaults to no timeout, which is troublesome if we ever drop our network
         * connection.  Give it a generous timeout to keep things moving. */
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(TIMEOUT_MILLIS);
        clientParams.setConnectionManagerTimeout(TIMEOUT_MILLIS);
        _httpClient.setParams(clientParams);

        /* Configure the multi-threaded connection manager. Default to MAX_INT (eg, unlimited)
         * connections, as AWS is intended to support such use */
        HttpConnectionManagerParams managerParam = new HttpConnectionManagerParams();
        MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        managerParam.setDefaultMaxConnectionsPerHost(Integer.MAX_VALUE);
        managerParam.setMaxTotalConnections(Integer.MAX_VALUE);
        manager.setParams(managerParam);
        _httpClient.setHttpConnectionManager(manager);
    }

    /**
     * Invalidate an object in the cloud. This forcibly removes cached copies on leaf nodes
     * without waiting for expiration. It is typically used when emergency changes happen to
     * important objects, or for immediate deletions (e.g. takedown notices).
     *
     * Amazon allows a large but finite (currently 1,000) invalidations per month for free,
     * after that there is a (very small) fee per invalidation. Systems that require instant
     * object updates as a matter of course should use object versioning instead.
     */
    public void invalidateObjects (String distributionId, Iterable<String> keys)
        throws CloudfrontException
    {
        PostMethod method = new PostMethod(buildActionURI(distributionId, ACTION_INVALIDATE));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(baos));
        XMLWriter writer = new XMLWriter(out);

        try {
            writer.startDocument();
            writer.startElement("InvalidationBatch");
            for (String key : keys) {
                writer.dataElement("Path", key);
            }
            writer.dataElement("CallerReference", String.valueOf(System.nanoTime()));
            writer.endElement("InvalidationBatch");
            writer.endDocument();
            writer.flush();

        } catch (Exception e) {
            throw new RuntimeException("Error encoding XML: " + e.getMessage(), e);
        }

        InputStream in = new ByteArrayInputStream(baos.toByteArray());
        method.setRequestEntity(new InputStreamRequestEntity(in, "text/xml"));

        signCloudfrontRequest(method);

        try {
            executeMethod(method);
        } finally {
            method.releaseConnection();
        }
    }

    protected void executeMethod (HttpMethod method)
        throws CloudfrontException
    {
        // Execute the request
        int statusCode;
        try {
            statusCode = _httpClient.executeMethod(method);

        } catch (IOException ioe) {
            throw new CloudfrontException("Network error executing Cloudfront method: " +
                ioe.getMessage(), ioe);
        }
        
        if (!(statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
            // Request failed, throw exception.
            InputStream stream;
            byte[] errorDoc = new byte[MAX_ERROR_SIZE];

            try {
                stream = method.getResponseBodyAsStream();
                if (stream == null) {
                    // We should always receive a response!
                    throw new CloudfrontException("Cloudfront failed to return an error " +
                        "response for HTTP status code: "+ statusCode);
                }

                stream.read(errorDoc, 0, errorDoc.length);
            } catch (IOException ioe) {
                throw new CloudfrontException("Network error receiving Cloudfront error response: " + ioe.getMessage(), ioe);
            }

            throw new CloudfrontException("Cloudfront error response: " + new String(errorDoc).trim());
        }
    }
    // http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/index.html?RESTAuthentication.html
    protected void signCloudfrontRequest (HttpMethod method)
    {
        // Set the required Date header (now)
        String date = rfc822Date(new Date());
        method.setRequestHeader("Date", date);

        // Sign and encode the Date header, which is all Cloudfront authorization requires
        SecretKeySpec signingKey = new SecretKeySpec(_secretKey.getBytes(), HMAC_SHA1_ALGORITHM);

        // Initialize a MAC instance with the signing key
        Mac mac;
        try {
            mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            throw new RuntimeException("Could not find SHA-1 algorithm.");
        }

        try {
            mac.init(signingKey);
        } catch (InvalidKeyException e) {
            // Also should not happen
            throw new RuntimeException("Could not initialize the MAC algorithm.", e);
        }

        // Compute the HMAC
        String b64 = new String(Base64.encodeBase64(mac.doFinal(date.getBytes())));

        // Insert the header
        method.setRequestHeader(AUTH_HEADER, "AWS " + _keyId + ":" + b64);
    }

    protected static String rfc822Date (Date date) {
        // Convert the expiration date to rfc822 format.
        final String DateFormat = "EEE, dd MMM yyyy HH:mm:ss ";
        SimpleDateFormat format;

        format = new SimpleDateFormat( DateFormat, Locale.US );
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date) + "GMT";
    }

    // e.g. POST /2010-08-01/distribution/[distribution ID]/invalidation HTTP/1.0
    protected String buildActionURI (String distributionId, String action)
    {
        return REQUEST_URI_PREFIX + "/" + distributionId + "/" + action;
    }

    protected static HostConfiguration createDefaultHostConfig ()
    {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(DEFAULT_HOST, HTTPS_PROTOCOL.getDefaultPort(), HTTPS_PROTOCOL);
        return hostConfig;
    }

    protected String _keyId;
    protected String _secretKey;

    protected HttpClient _httpClient;

    /** HTTPS protocol instance. */
    protected static final Protocol HTTPS_PROTOCOL = Protocol.getProtocol("https");

    /** Default Cloudfront host. */
    protected static final String DEFAULT_HOST = "cloudfront.amazonaws.com";

    /** Distribution API prefix for the request URI. */
    protected static final String REQUEST_URI_PREFIX = "/2010-08-01/distribution";

    /** INVALIDATE Action postfix for the request URI. */
    protected static final String ACTION_INVALIDATE = "invalidation";

    /** Maximum size of error output. Should never be larger than 2k!!! */
    private static final int MAX_ERROR_SIZE = 2048;

    /** AWS Authorization Header Name. */
    protected static final String AUTH_HEADER = "Authorization";

    /** HMAC/SHA1 Algorithm per RFC 2104. */
    protected static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /** Connection and read timeout for our http connections in milliseconds. */
    protected static final int TIMEOUT_MILLIS = 2 * 60 * 1000;
}
