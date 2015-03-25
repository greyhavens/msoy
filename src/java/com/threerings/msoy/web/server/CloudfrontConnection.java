//
// $Id: $

package com.threerings.msoy.web.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;

import com.samskivert.util.StringUtil;

/**
 * An interface into the Cloudfront system. It is initially configured with
 * authentication and connection parameters and exposes methods to access and
 * manipulate Cloudfront distributions and object invalidation.
 */
public abstract class CloudfrontConnection
{
    public static class Tagged<T extends ComplexType<T>>
    {
        public String eTag;
        public T result;

        public static <T extends ComplexType<T>> Tagged<T> tag (GetMethod method, T result)
            throws CloudfrontException
        {
            Header header = method.getResponseHeader("ETag");
            if (header == null) {
                throw new CloudfrontException("No ETag header on response!");
            }
            return new Tagged<T>(header.getValue(), result);
        }

        public Tagged (String eTag, T result)
        {
            this.eTag = eTag;
            this.result = result;
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }
    }

    protected CloudfrontConnection (String keyId, String secretKey)
    {
        this(keyId, secretKey, createDefaultHostConfig());
    }

    protected CloudfrontConnection (
        String keyId, String secretKey, HostConfiguration hostConfig)
    {
        _keyId = keyId;
        _secretKey = secretKey;
        _httpClient = new HttpClient();
        _httpClient.setHostConfiguration(hostConfig);
    }

    protected <R extends ComplexType<R>, W extends WriteableComplexType<W>> R execute (
        EntityEnclosingMethod method, Tagged<W> taggedType, ReturnBodyParser<R> parser)
            throws CloudfrontException
    {
        method.setRequestHeader("If-Match", taggedType.eTag);
        return execute(method, taggedType.result, parser);
    }

    protected <T> T execute (
        EntityEnclosingMethod method, RequestBodyConstructor constructor,
        ReturnBodyParser<T> parser)
            throws CloudfrontException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            CloudfrontEventWriter writer = new CloudfrontEventWriter(
                _xmlOutputFactory.createXMLEventWriter(baos));
            writer.startDocument();
            constructor.constructBody(writer);
            writer.endDocument();

        } catch (Exception e) {
            throw new CloudfrontException("Error encoding XML: " + e.getMessage(), e);
        }
        byte[] bytes = baos.toByteArray();
        method.setRequestEntity(new ByteArrayRequestEntity(bytes, "text/xml"));
        // log.info("Sending request", "body", new String(bytes));
        return execute(method, parser);
    }

    protected <T> T execute (HttpMethod method, ReturnBodyParser<T> parser)
        throws CloudfrontException
    {
        try {
            InputStream stream = doExecute(method);
            if (parser != null) {
                CloudfrontEventReader reader =
                    new CloudfrontEventReader(_xmlInputFactory.createXMLEventReader(stream));
                reader.expectType(XMLStreamConstants.START_DOCUMENT);
                T val = parser.parseBody(reader);
                reader.expectType(XMLStreamConstants.END_DOCUMENT);
                return val;
            }
            return null;

        } catch (XMLStreamException e) {
            throw new CloudfrontException("Network error executing method", e);

        } finally {
            method.releaseConnection();
        }
    }

    protected InputStream doExecute (HttpMethod method)
        throws CloudfrontException
    {
        signCloudfrontRequest(method);

        // Execute the request
        int statusCode;
        try {
            // log.info ("Executing HTTP method", "uri", method.getURI());
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
        String date = RFC822_DATE_FORMAT.format(new Date());
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

    protected static HostConfiguration createDefaultHostConfig ()
    {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(DEFAULT_HOST, HTTPS_PROTOCOL.getDefaultPort(), HTTPS_PROTOCOL);
        return hostConfig;
    }

    protected static abstract class ContainerElement
    {
        public void recurseInto (CloudfrontEventReader reader, String elementName)
            throws XMLStreamException
        {
            reader.expectElementStart(elementName);
            do {
                if (!parseNextElement(reader)) {
                    throw new XMLStreamException("Unexpected event: " + reader.peek());
                }
            } while (!(reader.peek() instanceof EndElement));
            reader.expectElementEnd(elementName);
        }

        public abstract boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException;
    }

    protected static abstract class ComplexType<T extends ComplexType<T>>
        extends ContainerElement implements ReturnBodyParser<T>
    {
        public T initialize (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            recurseInto(reader, typeElement());
            if (!isComplete()) {
                throw new XMLStreamException("Got partial object: " + this);
            }

            @SuppressWarnings("unchecked")
            T tThis = (T) this;
            return tThis;
        }

        public T parseBody (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            return initialize(reader);
        }

        public String toString ()
        {
            return StringUtil.fieldsToString(this);
        }

        protected abstract String typeElement ();
        protected abstract boolean isComplete ();
    }

    protected static abstract class WriteableComplexType<T extends WriteableComplexType<T>>
        extends ComplexType<T> implements RequestBodyConstructor
    {
        public void constructBody (CloudfrontEventWriter writer) throws XMLStreamException
        {
            writer.startElement(typeElement());
            writeElements(writer);
            writer.endElement(typeElement());
        }

        public abstract void writeElements (CloudfrontEventWriter writer)
            throws XMLStreamException;
    }

    protected abstract static class ElementListBuilder<T extends ComplexType<T>>
        extends ContainerElement
        implements ReturnBodyParser<List<T>>
    {
        public ElementListBuilder (String listElement)
        {
            _listElement = listElement;
            _partElement = createElement().typeElement();
        }

        public List<T> parseBody (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            recurseInto(reader, _listElement);
            return _result;
        }

        public boolean parseNextElement (CloudfrontEventReader reader) throws XMLStreamException {
            if (reader.maybeSkip("Marker", "NextMarker", "MaxItems", "IsTruncated")) {
                // nothing to do
            } else if (reader.peekForElement(_partElement)) {
                _result.add(createElement().initialize(reader));
            } else {
                return false;
            }
            return true;
        }

        protected abstract T createElement ();

        protected final String _listElement, _partElement;
        protected final List<T> _result = Lists.newArrayList();
    }

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

    protected interface RequestBodyConstructor
    {
        public void constructBody (CloudfrontEventWriter writer) throws XMLStreamException;
    }

    protected interface ReturnBodyParser<T>
    {
        public T parseBody (CloudfrontEventReader writer) throws XMLStreamException;
    }

    protected String _keyId;
    protected String _secretKey;

    protected XMLInputFactory _xmlInputFactory = XMLInputFactory.newInstance();
    protected XMLOutputFactory _xmlOutputFactory = XMLOutputFactory.newInstance();

    protected HttpClient _httpClient;

    /** HTTPS protocol instance. */
    protected static final Protocol HTTPS_PROTOCOL = Protocol.getProtocol("https");

    /** Default Cloudfront host. */
    protected static final String DEFAULT_HOST = "cloudfront.amazonaws.com";

    /** Maximum size of error output. Should never be larger than 2k!!! */
    private static final int MAX_ERROR_SIZE = 2048;

    /** AWS Authorization Header Name. */
    protected static final String AUTH_HEADER = "Authorization";

    /** HMAC/SHA1 Algorithm per _RF 2104. */
    protected static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /** Connection and read timeout for our http connections in milliseconds. */
    protected static final int TIMEOUT_MILLIS = 2 * 60 * 1000;

    protected static final DateFormat RFC822_DATE_FORMAT;
    static {
        RFC822_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        RFC822_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
