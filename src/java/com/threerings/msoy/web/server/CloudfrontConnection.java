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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.megginson.sax.XMLWriter;
import org.xml.sax.SAXException;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.ServerConfig;

import static com.threerings.msoy.Log.log;

/**
 * An interface into the Cloudfront system. It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate Cloudfront distributions and object invalidation.
 *
 * Functionality that remains unimplemented:
 * POST   /2010-08-01/distribution
 * PUT    /2010-08-01/distribution/DistID/config
 * DELETE /2010-08-01/distribution/DistID
 * 
 * PUT    /2010-08-01/origin-access-identity/cloudfront/IdentityID/config
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
        CloudfrontConnection conn = new CloudfrontConnection(
            ServerConfig.cloudId, ServerConfig.cloudKey);
        try {
            String body = null;
            List<?> bits = null;
            if (args.length > 0) {
                String cmd = args[0];
                if ("dists".equals(cmd)) {
                    bits = conn.getDistributions();
                } else if ("oaids".equals(cmd)) {
                    bits = conn.getOriginAccessIdentities();
                }
                if (args.length > 1) {
                    if ("invreqs".equals(cmd)) {
                        body = conn.getInvalidations(args[1]);
                    } else if ("dist".equals(cmd)) {
                        body = conn.getDistribution(args[1]);
                    } else if ("distconf".equals(cmd)) {
                        body = conn.getDistributionConfig(args[1]);
                    } else if ("oaid".equals(cmd)) {
                        body = conn.getOriginAccessIdentity(args[1]);
                    }
                }
                if (args.length > 2) {
                    if ("invalidate".equals(cmd)) {
                        body = conn.invalidateObjects(args[1], Collections.singleton(args[2]));
                    } else if ("invreq".equals(cmd)) {
                        body = conn.getInvalidation(args[1], args[2]);
                    }
                }
            }

            if (bits != null) {
                body = Joiner.on("\n").join(bits);
            }

            if (body == null) {
                System.err.println(
                    "Available commands:\n" +
                    "dists\n" +
                    "dist <distId>\n" +
                    "distconf <distId>\n" +
                    "oaids\n" +
                    "oaid <id>\n" +
                    "invreqs\n" +
                    "invreq <batchId>\n" +
                    "invalidate <distId> <key>");
                return;
            }
            System.out.println("Result: " + body);
        } catch (CloudfrontException e) {
            e.printStackTrace();
        }
    }

    public interface CloudFrontComplexType
    {
        public boolean isComplete ();
    }

    public static class OriginAccessIdentitySummary
        implements CloudFrontComplexType
    {
        public String id;
        public String s3CanonicalUserId;
        public String comment;

        public static OriginAccessIdentitySummary create (XMLEventReader reader)
            throws XMLStreamException
        {
            OriginAccessIdentitySummary summary = new OriginAccessIdentitySummary();

            expectElementStart(reader, "CloudFrontOriginAccessIdentitySummary");
            while (reader.hasNext()) {
                String data;

                if (null != (data = maybeReadElement(reader, "Id"))) {
                    summary.id = data;

                } else if (null != (data = maybeReadElement(reader, "S3CanonicalUserId"))) {
                    summary.s3CanonicalUserId = data;

                } else if (null != (data = maybeReadElement(reader, "Comment"))) {
                    summary.comment = data;

                } else if (reader.peek() instanceof EndElement) {
                    expectElementEnd(reader, "CloudFrontOriginAccessIdentitySummary");
                    if (!summary.isComplete()) {
                        throw new XMLStreamException("Got partial object: " + summary);
                    }
                    return summary;

                } else {
                    throw new XMLStreamException("Unexpected event: " + reader.peek());
                }
            }
            throw new XMLStreamException("Unexpected end of XML stream.");
        }

        public boolean isComplete ()
        {
            return id != null && s3CanonicalUserId != null;
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    public static class DistributionSummary
        implements CloudFrontComplexType
    {
        public String id;
        public String status;
        public String lastModifiedTime;
        public String domainName;
        public String origin;
        public Set<String> cnames = Sets.newHashSet();
        public String comment;
        public String enabled;
        public List<String> trustedSigners = Lists.newArrayList();

        public static DistributionSummary create (XMLEventReader reader)
            throws XMLStreamException
        {
            DistributionSummary summary = new DistributionSummary();

            expectElementStart(reader, "DistributionSummary");
            do {
                String data;

                if (null != (data = maybeReadElement(reader, "Id"))) {
                    summary.id = data;

                } else if (null != (data = maybeReadElement(reader, "Status"))) {
                    summary.status = data;

                } else if (null != (data = maybeReadElement(reader, "LastModifiedTime"))) {
                    summary.lastModifiedTime = data;

                } else if (null != (data = maybeReadElement(reader, "DomainName"))) {
                    summary.domainName = data;

                } else if (null != (data = maybeReadElement(reader, "CNAME"))) {
                    summary.cnames.add(data);

                } else if (null != (data = maybeReadElement(reader, "Origin"))) {
                    summary.origin = data;

                } else if (null != (data = maybeReadElement(reader, "Comment"))) {
                    summary.comment = data;

                } else if (null != (data = maybeReadElement(reader, "Enabled"))) {
                    summary.enabled = data;

                } else if (peekForElement(reader, "TrustedSigners")) {
                    reader.nextEvent();
                    do {
                        if (null != (data = maybeReadElement(reader, "Self"))) {
                            summary.trustedSigners.add("SELF");
                        } else if (null != (data = maybeReadElement(reader, "AwsAccountNumber"))) {
                            summary.trustedSigners.add("data");
                        } else {
                            throw new XMLStreamException("Unexpected event: " + reader.peek());
                        }
                    } while (!(reader.peek() instanceof EndElement));
                    expectElementEnd(reader, "TrustedSigners");                    

                } else {
                    throw new XMLStreamException("Unexpected event: " + reader.peek());
                }
            } while (!(reader.peek() instanceof EndElement));

            expectElementEnd(reader, "DistributionSummary");
            if (!summary.isComplete()) {
                throw new XMLStreamException("Got partial object: " + summary);
            }
            return summary;
        }

        public boolean isComplete ()
        {
            return id != null && status != null && lastModifiedTime != null && domainName != null
                && origin != null && enabled != null;
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    public List<OriginAccessIdentitySummary> getOriginAccessIdentities ()
        throws CloudfrontException
    {
        // GET /2010-08-01/origin-access-identity/cloudfront?Marker=value&MaxItems=value
        GetMethod method = new GetMethod(API.ORIGIN_ACCESS_ID.build("cloudfront"));

        return executeAndReturn(method, new ReturnBodyParser<List<OriginAccessIdentitySummary>>() {
            public List<OriginAccessIdentitySummary> parseBody (XMLEventReader reader)
                throws XMLStreamException
            {
                expectElementStart(reader, "CloudFrontOriginAccessIdentityList");
                List<OriginAccessIdentitySummary> result = Lists.newArrayList();
                while (reader.hasNext()) {
                    if (maybeSkip(reader, "Marker", "NextMarker", "MaxItems", "IsTruncated")) {
                        // nothing to do

                    } else if (peekForElement(reader, "CloudFrontOriginAccessIdentitySummary")) {
                        result.add(OriginAccessIdentitySummary.create(reader));

                    } else if (reader.peek() instanceof EndElement) {
                        expectElementEnd(reader, "CloudFrontOriginAccessIdentityList");
                        return result;

                    } else {
                        throw new XMLStreamException("Unexpected event: " + reader.peek());
                    }
                }
                throw new XMLStreamException("Unexpected end of XML stream.");
            }
        });
    }

    public String getOriginAccessIdentity (String id)
        throws CloudfrontException
    {
        // GET /2010-08-01/origin-access-identity/cloudfront/IdentityID
        GetMethod method = new GetMethod(API.ORIGIN_ACCESS_ID.build("cloudfront", id));
        return executeAndReturn(method, null);
    }

    public String getOriginAccessIdentityConfig (String id)
        throws CloudfrontException
    {
        // GET /2010-08-01/origin-access-identity/cloudfront/IdentityID/config
        GetMethod method = new GetMethod(
            API.ORIGIN_ACCESS_ID.build("cloudfront", id, "config"));
        return executeAndReturn(method, null);
    }

    public List <DistributionSummary> getDistributions ()
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution?Marker=value&MaxItems=value
        GetMethod method = new GetMethod(API.DISTRIBUTION.build());

        return executeAndReturn(method, new ReturnBodyParser<List<DistributionSummary>>() {
            public List<DistributionSummary> parseBody (XMLEventReader reader)
                throws XMLStreamException
            {
                expectElementStart(reader, "DistributionList");
                List<DistributionSummary> result = Lists.newArrayList();
                while (reader.hasNext()) {
                    if (maybeSkip(reader, "Marker", "NextMarker", "MaxItems", "IsTruncated")) {
                        // nothing to do

                    } else if (peekForElement(reader, "DistributionSummary")) {
                        result.add(DistributionSummary.create(reader));

                    } else if (reader.peek() instanceof EndElement) {
                        expectElementEnd(reader, "DistributionList");
                        return result;

                    } else {
                        throw new XMLStreamException("Unexpected event: " + reader.peek());
                    }
                }
                throw new XMLStreamException("Unexpected end of XML stream.");
            }
        });
    }

    public String getDistribution (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution));
        return executeAndReturn(method, null);
    }

    public String getDistributionConfig (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/config
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution, "config"));
        return executeAndReturn(method, null);
    }

    public String getInvalidations (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/invalidation?Marker=value&MaxItems=value
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution, "invalidation"));
        return executeAndReturn(method, null);
    }

    public String getInvalidation (String distribution, String batch)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/invalidation/invalidationID
        GetMethod method = new GetMethod(
            API.DISTRIBUTION.build(distribution, "invalidation", batch));
        return executeAndReturn(method, null);
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
    public String invalidateObjects (String distribution, final Iterable<String> keys)
        throws CloudfrontException
    {
        // POST /2010-08-01/distribution/DistID/invalidation
        return executeWithBody(
            new PostMethod(API.DISTRIBUTION.build(distribution, "invalidation")),
            new RequestBodyConstructor() {
                public void constructBody (XMLWriter writer) throws SAXException {
                    writer.startElement("InvalidationBatch");
                    for (String key : keys) {
                        writer.dataElement("Path", key);
                    }
                    writer.dataElement("CallerReference", String.valueOf(System.nanoTime()));
                    writer.endElement("InvalidationBatch");
                }
            },
            null);
    }

    public String createOriginAccessIdentity (final String comment)
        throws CloudfrontException
    {
        // POST /2010-08-01/origin-access-identity/cloudfront
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
            },
            null);
    }

    public String deleteOriginAccessIdentity (String distribution, String tag)
        throws CloudfrontException
    {
        // DELETE /2010-08-01/origin-access-identity/cloudfront/IdentityID
        DeleteMethod method = new DeleteMethod(
            API.ORIGIN_ACCESS_ID.build("cloudfront", distribution));
        method.addRequestHeader("If-Match", tag);
        signCloudfrontRequest(method);

        return executeAndReturn(method, null);
    }

    protected interface RequestBodyConstructor
    {
        public void constructBody (XMLWriter writer) throws SAXException;
    }

    protected interface ReturnBodyParser<T>
    {
        public T parseBody (XMLEventReader writer) throws XMLStreamException;
    }

    protected <T> T executeWithBody (
        EntityEnclosingMethod method, RequestBodyConstructor constructor,
        ReturnBodyParser<T> parser)
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

        return executeAndReturn(method, parser);
    }

    protected <T> T executeAndReturn (HttpMethod method, ReturnBodyParser<T> parser)
        throws CloudfrontException
    {
        signCloudfrontRequest(method);
        try {
            InputStream stream = execute(method);
            if (parser != null) {
                XMLEventReader reader = _xmlFactory.createXMLEventReader(stream);
                expectType(reader, XMLStreamConstants.START_DOCUMENT);
                T val = parser.parseBody(reader);
                expectType(reader, XMLStreamConstants.END_DOCUMENT);
                return val;
            }
            return null;

        } catch (XMLStreamException e) {
            throw new CloudfrontException("Network error executing method", e);

        } finally {
            method.releaseConnection();
        }
    }

    protected InputStream execute (HttpMethod method)
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

    protected static void expectType (XMLEventReader reader, int eventType)
        throws XMLStreamException
    {
        XMLEvent event = reader.nextEvent();
        if (event.getEventType() != eventType) {
            throw new XMLStreamException("Expecting event type [" + eventType + "], got " + event);
        }
    }

    protected static void expectElementStart (XMLEventReader reader, String elementName)
        throws XMLStreamException
    {
        XMLEvent event = reader.nextEvent();
        if ((event instanceof StartElement) &&
            ((StartElement) event).getName().getLocalPart().equals(elementName)) {
            log.info("Expected and found element: " + elementName);
            return;
        }
        throw new XMLStreamException("Expecting start of element [" + elementName + "], got " + event);
    }

    protected static void expectElementEnd (XMLEventReader reader, String elementName)
        throws XMLStreamException
    {
        XMLEvent event = reader.nextEvent();
        if ((event instanceof EndElement) &&
            ((EndElement) event).getName().getLocalPart().equals(elementName)) {
            return;
        }
        throw new XMLStreamException("Expecting end of element [" + elementName + "], got " + event);
    }

    protected static boolean peekForElement (XMLEventReader reader, String element)
        throws XMLStreamException
    {
        XMLEvent event = reader.peek();
        return ((event instanceof StartElement) &&
                element.equals(((StartElement) event).getName().getLocalPart()));
    }

    protected static boolean maybeSkip (XMLEventReader reader, String... elements)
        throws XMLStreamException
    {
        XMLEvent event = reader.peek();
        if (event instanceof StartElement) {
            String name = ((StartElement) event).getName().getLocalPart();
            if (Arrays.asList(elements).contains(name)) {
                expectElementStart(reader, name);
                if (reader.peek() instanceof Characters) {
                    reader.nextEvent();
                }
                expectElementEnd(reader, name);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns null if the specified element is not in fact the next thing in front of our cursor;
     * returns empty string for matching but empty elements.
     */
    protected static String maybeReadElement (XMLEventReader reader, String element)
        throws XMLStreamException
    {
        if (!peekForElement(reader, element)) {
            return null;
        }
        reader.nextEvent();

        String result;
        XMLEvent event = reader.peek();
        if (event instanceof Characters) {
            result = ((Characters) event).getData();
            event = reader.nextEvent();
        } else {
            result = "";
        }
        expectElementEnd(reader, element);
        log.info("Returning character content: " + result);
        return result;
    }

    protected String _keyId;
    protected String _secretKey;

    protected XMLInputFactory _xmlFactory = XMLInputFactory.newInstance();

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

    /** Maximum size of error output. Should never be larger than 2k!!! */
    private static final int MAX_ERROR_SIZE = 2048;

    /** AWS Authorization Header Name. */
    protected static final String AUTH_HEADER = "Authorization";

    /** HMAC/SHA1 Algorithm per RFC 2104. */
    protected static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /** Connection and read timeout for our http connections in milliseconds. */
    protected static final int TIMEOUT_MILLIS = 2 * 60 * 1000;
}
