//
// $Id: $

package com.threerings.msoy.web.server;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;

import com.threerings.msoy.server.ServerConfig;

import com.megginson.sax.XMLWriter;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.xml.sax.SAXException;

import static com.threerings.msoy.Log.log;

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

    public static final void main (String[] args)
    {
        CloudfrontConnection foo = new CloudfrontConnection(ServerConfig.cloudId, ServerConfig.cloudKey);
        try {
            System.out.println(foo.getDistribution(ServerConfig.cloudDistribution));
            System.exit(1);
        } catch (CloudfrontException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public String getOriginAccessIdentities ()
        throws CloudfrontException
    {
        GetMethod method = new GetMethod(API.ORIGIN_ACCESS_ID.build("cloudfront"));
        return foofoofoo(method);
    }

    public String getDistributions ()
        throws CloudfrontException
    {
        GetMethod method = new GetMethod(API.DISTRIBUTION.build());
        return foofoofoo(method);
    }

    public String getDistribution (String distribution)
        throws CloudfrontException
    {
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution));
        return foofoofoo(method);
    }

    public String getDistributionConfig (String distribution)
        throws CloudfrontException
    {
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution, "config"));
        return foofoofoo(method);
    }


    protected String foofoofoo (HttpMethod method)
        throws CloudfrontException
    {
        signCloudfrontRequest(method);
        try {
            InputStream stream = executeMethod(method);
            return CharStreams.toString(new InputStreamReader(stream));

        } catch (IOException e) {
            throw new CloudfrontException("Network error executing method", e);

        } finally {
            method.releaseConnection();
        }
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
    public void invalidateObjects (String distributionId, final Iterable<String> keys)
        throws CloudfrontException
    {
        executeWithBody(
            new PostMethod(API.DISTRIBUTION.build(distributionId, "invalidation")),
            new RequestBodyConstructor() {
                public void constructBody (XMLWriter writer) throws SAXException {
                    writer.startElement("InvalidationBatch");
                    for (String key : keys) {
                        writer.dataElement("Path", key);
                    }
                    writer.dataElement("CallerReference", String.valueOf(System.nanoTime()));
                    writer.endElement("InvalidationBatch");
                }
            });
    }

    public String createOriginAccessIdentity (final String comment)
        throws CloudfrontException
    {
        return executeWithBody(
            new PostMethod(API.ORIGIN_ACCESS_ID.build("cloudfront")),
            new RequestBodyConstructor() {
                public void constructBody (XMLWriter writer) throws SAXException
                {
                    writer.startElement("CloudFrontOriginAccessIdentityConfig");
                    writer.dataElement("Comment", comment);
                    writer.dataElement("CallerReference", String.valueOf(System.nanoTime()));
                    writer.endElement("CloudFrontOriginAccessIdentityConfig");
                }
            });
    }

    public String deleteOriginAccessIdentity (String distributionId, String tag)
        throws CloudfrontException
    {
        DeleteMethod method = new DeleteMethod(
            API.ORIGIN_ACCESS_ID.build("cloudfront", distributionId));

        signCloudfrontRequest(method);

        return foofoofoo(method);
    }

    
    protected interface RequestBodyConstructor
    {
        public void constructBody (XMLWriter writer) throws SAXException;
    }

    protected String executeWithBody (PostMethod method, RequestBodyConstructor constructor)
        throws CloudfrontException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(baos));
        XMLWriter writer = new XMLWriter(out);

        try {
            writer.startDocument();
            constructor.constructBody(writer);
            writer.endDocument();
            writer.flush();

        } catch (Exception e) {
            throw new RuntimeException("Error encoding XML: " + e.getMessage(), e);
        }

        InputStream in = new ByteArrayInputStream(baos.toByteArray());
        method.setRequestEntity(new InputStreamRequestEntity(in, "text/xml"));

        signCloudfrontRequest(method);

        return foofoofoo(method);
    }

    protected InputStream executeMethod (HttpMethod method)
        throws CloudfrontException
    {
        // Execute the request
        int statusCode;
        try {
            log.info ("Executing HTTP method", "uri", method.getURI());
            statusCode = _httpClient.executeMethod(method);

        } catch (IOException ioe) {
            throw new CloudfrontException("Network error executing Cloudfront method: " +
                ioe.getMessage(), ioe);
        }

        InputStream stream;
        try {
            stream = method.getResponseBodyAsStream();

            if (!(statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES)) {
                // Request failed, throw exception.
                byte[] errorDoc = new byte[MAX_ERROR_SIZE];

                if (stream == null) {
                    // We should always receive a response!
                    throw new CloudfrontException("Cloudfront failed to return an error " +
                        "response for HTTP status code: "+ statusCode);
                }

                stream.read(errorDoc, 0, errorDoc.length);
                throw new CloudfrontException("Cloudfront error response: " + new String(errorDoc).trim());
            }

            return stream;
        } catch (IOException ioe) {
             throw new CloudfrontException("Network error receiving Cloudfront error response: " + ioe.getMessage(), ioe);
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

    protected static HostConfiguration createDefaultHostConfig ()
    {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(DEFAULT_HOST, HTTPS_PROTOCOL.getDefaultPort(), HTTPS_PROTOCOL);
        return hostConfig;
    }

    protected String _keyId;
    protected String _secretKey;

    protected HttpClient _httpClient;

    protected enum API
    {
        DISTRIBUTION("/2010-08-01/distribution"),
        ORIGIN_ACCESS_ID("/2010-08-01/origin-access-identity");

        API (String requestPrefix)
        {
            _prefix = requestPrefix;
        }

        public String getRequestPrefix ()
        {
            return _prefix;
        }

        public String build (String... args)
        {
            if (args.length == 0) {
                return _prefix;
            }
            return _prefix + "/" + Joiner.on("/").join(args);
        }

        protected String _prefix;
    }

    /** HTTPS protocol instance. */
    protected static final Protocol HTTPS_PROTOCOL = Protocol.getProtocol("https");

    /** Default Cloudfront host. */
    protected static final String DEFAULT_HOST = "cloudfront.amazonaws.com";

    /** Origin Access Identify API prefix for the request URI. */


    /** Maximum size of error output. Should never be larger than 2k!!! */
    private static final int MAX_ERROR_SIZE = 2048;

    /** AWS Authorization Header Name. */
    protected static final String AUTH_HEADER = "Authorization";

    /** HMAC/SHA1 Algorithm per RFC 2104. */
    protected static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /** Connection and read timeout for our http connections in milliseconds. */
    protected static final int TIMEOUT_MILLIS = 2 * 60 * 1000;
}
