//
// $Id: $

package com.threerings.msoy.web.server;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 *
 */
public class OriginAccessIdentityAPI extends CloudfrontConnection
{
    public static class OriginAccessIdentitySummary
        extends ComplexType<OriginAccessIdentitySummary>
    {
        public String id;
        public String s3CanonicalUserId;
        public String comment;

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("S3CanonicalUserId"))) {
                s3CanonicalUserId = str;
            } else if (null != (str = reader.maybeString("Comment"))) {
                comment = str;
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "CloudFrontOriginAccessIdentitySummary";
        }

        public boolean isComplete ()
        {
            return id != null && s3CanonicalUserId != null;
        }
    }

    public static class OriginAccessIdentity
        extends ComplexType<OriginAccessIdentity>
    {
        public String id;
        public String s3CanonicalUserId;
        public OriginAccessIdentityConfig config;

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("S3CanonicalUserId"))) {
                s3CanonicalUserId = str;
            } else if (reader.peekForElement("CloudFrontOriginAccessIdentityConfig")) {
                config = new OriginAccessIdentityConfig().initialize(reader);
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "CloudFrontOriginAccessIdentity";
        }

        public boolean isComplete ()
        {
            return id != null && s3CanonicalUserId != null && config != null;
        }
    }

    public static class OriginAccessIdentityConfig
        extends WriteableComplexType<OriginAccessIdentityConfig>
    {
        public String callerReference;
        public String comment;

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("CallerReference"))) {
                callerReference = str;
            } else if (null != (str = reader.maybeString("Comment"))) {
                comment = str;
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "CloudFrontOriginAccessIdentityConfig";
        }

        @Override
        public void writeElements (CloudfrontEventWriter writer)
            throws XMLStreamException
        {
            writer.writeString("CallerReference", callerReference);
            writer.writeString("Comment", comment);
        }

        public boolean isComplete ()
        {
            return callerReference != null;
        }
    }

    public OriginAccessIdentityAPI (String keyId, String secretKey)
    {
        super(keyId, secretKey);
    }

    public List<OriginAccessIdentitySummary> getOriginAccessIdentities ()
       throws CloudfrontException
   {
       // GET /2010-08-01/origin-access-identity/cloudfront?Marker=value&MaxItems=value
       GetMethod method = new GetMethod(API.ORIGIN_ACCESS_ID.build("cloudfront"));
       String listElement = "CloudFrontOriginAccessIdentityList";
       return execute(method, new ElementListBuilder<OriginAccessIdentitySummary>(listElement) {
           @Override protected OriginAccessIdentitySummary createElement () {
               return new OriginAccessIdentitySummary();
           }
       });
   }

    public OriginAccessIdentity getOriginAccessIdentity (String id)
        throws CloudfrontException
    {
        // GET /2010-08-01/origin-access-identity/cloudfront/IdentityID
        GetMethod method = new GetMethod(API.ORIGIN_ACCESS_ID.build("cloudfront", id));
        return execute(method, new OriginAccessIdentity());
    }

    public Tagged<OriginAccessIdentityConfig> getOriginAccessIdentityConfig (String id)
        throws CloudfrontException
    {
        // GET /2010-08-01/origin-access-identity/cloudfront/IdentityID/config
        GetMethod method = new GetMethod(
            API.ORIGIN_ACCESS_ID.build("cloudfront", id, "config"));
        OriginAccessIdentityConfig config = execute(method, new OriginAccessIdentityConfig());
        return Tagged.tag(method, config);
    }

    public OriginAccessIdentity createOriginAccessIdentity (final String comment)
        throws CloudfrontException
    {
        OriginAccessIdentityConfig config = new OriginAccessIdentityConfig();
        config.callerReference = String.valueOf(System.nanoTime());
        config.comment = comment;
        return postConfig(config);
    }

    public OriginAccessIdentity postConfig (OriginAccessIdentityConfig config)
        throws CloudfrontException
    {
        // POST /2010-08-01/origin-access-identity/cloudfront
        return execute(
            new PostMethod(API.ORIGIN_ACCESS_ID.build("cloudfront")),
            config, new OriginAccessIdentity());
    }

    public OriginAccessIdentity putConfig (String oaid, Tagged<OriginAccessIdentityConfig> config)
        throws CloudfrontException
    {
        // PUT /2010-08-01/origin-access-identity/cloudfront/IdentityID/config
        return execute(
            new PutMethod(API.ORIGIN_ACCESS_ID.build("cloudfront", oaid, "config")),
            config, new OriginAccessIdentity());
    }

    public void deleteOriginAccessIdentity (String oaid, String tag)
        throws CloudfrontException
    {
        // DELETE /2010-08-01/origin-access-identity/cloudfront/IdentityID
        DeleteMethod method = new DeleteMethod(API.ORIGIN_ACCESS_ID.build("cloudfront", oaid));
        method.addRequestHeader("If-Match", tag);
        execute(method, null);
    }
}
