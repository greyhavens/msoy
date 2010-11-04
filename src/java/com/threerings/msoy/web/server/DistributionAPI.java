//
// $Id: $

package com.threerings.msoy.web.server;

import javax.xml.stream.XMLStreamException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;

/**
 *
 */
public class DistributionAPI extends CloudfrontConnection
{
    public static class DistributionSummary
        extends ComplexType<DistributionSummary>
    {
        public String id;
        public String status;
        public Date lastModifiedTime;
        public String domainName;
        public String origin;
        public Set<String> cnames = Sets.newHashSet();
        public String comment;
        public Boolean enabled;
        public boolean selfIsSigner;
        public List<String> trustedAwsSigners = Lists.newArrayList();

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str; Date date; Boolean bool;

            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("Status"))) {
                status = str;
            } else if (null != (date = reader.maybeDate("LastModifiedTime"))) {
                lastModifiedTime = date;
            } else if (null != (str = reader.maybeString("DomainName"))) {
                domainName = str;
            } else if (null != (str = reader.maybeString("CNAME"))) {
                cnames.add(str);
            } else if (null != (str = reader.maybeString("Origin"))) {
                origin = str;
            } else if (null != (str = reader.maybeString("Comment"))) {
                comment = str;
            } else if (null != (bool = reader.maybeBoolean("Enabled"))) {
                enabled = bool;

            } else if (reader.peekForElement("TrustedSigners")) {
                new ContainerElement() {
                    public boolean parseNextElement (CloudfrontEventReader reader)
                        throws XMLStreamException {
                        String str;
                        if (null != (str = reader.maybeString("Self"))) {
                            selfIsSigner = true;
                        } else if (null != (str = reader.maybeString("AwsAccountNumber"))) {
                            trustedAwsSigners.add(str);
                        } else {
                            return false;
                        }
                        return true;
                    }
                }.recurseInto(reader, "TrustedSigners");
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "DistributionSummary";
        }

        public boolean isComplete ()
        {
            return id != null && status != null && lastModifiedTime != null && domainName != null
                && origin != null && enabled != null;
        }
    }

    public static class Signer
        extends ComplexType<Signer>
    {
        public boolean isSelf;
        public String awsAccountNumber;
        public Set<String> keyIds = Sets.newHashSet();

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Self"))) {
                isSelf = true;
            } else if (null != (str = reader.maybeString("AwsAccountNumber"))) {
                awsAccountNumber = str;
            } else if (null != (str = reader.maybeString("KeyPairId"))) {
                keyIds.add(str);
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "Signer";
        }

        public boolean isComplete ()
        {
            return isSelf || (awsAccountNumber != null);
        }
    }

    public static class Distribution
        extends ComplexType<Distribution>
    {
        public String id;
        public String status;
        public Integer inProgressInvalidationBatches;
        public Date lastModifiedTime;
        public String domainName;
        public List<Signer> activeTrustedSigners = Lists.newArrayList();
        public DistributionConfig config;

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str; Date date; Integer n;

            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("Status"))) {
                status = str;
            } else if (null != (n = reader.maybeInt("InProgressInvalidationBatches")))  {
                inProgressInvalidationBatches = n;
            } else if (null != (date = reader.maybeDate("LastModifiedTime"))) {
                lastModifiedTime = date;
            } else if (null != (str = reader.maybeString("DomainName"))) {
                domainName = str;

            } else if (reader.peekForElement("ActiveTrustedSigners")) {
                new ContainerElement() {
                    public boolean parseNextElement (CloudfrontEventReader reader) throws XMLStreamException {
                        if (reader.peekForElement("Signer")) {
                            activeTrustedSigners.add(new Signer().initialize(reader));
                            return true;
                        }
                        return false;
                    }
                }.recurseInto(reader, "ActiveTrustedSigners");

            } else if (reader.peekForElement("DistributionConfig")) {
                config = new DistributionConfig().initialize(reader);
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "Distribution";
        }

        public boolean isComplete ()
        {
            return id != null && status != null && lastModifiedTime != null && domainName != null
                && config != null;
        }
    }

    public static class Logging
        extends WriteableComplexType<Logging>
    {
        public String bucket;
        public String prefix;

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Bucket"))) {
                bucket = str;
            } else if (null != (str = reader.maybeString("Prefix"))) {
                prefix = str;
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "Logging";
        }

        @Override
        public void writeElements (CloudfrontEventWriter writer)
            throws XMLStreamException
        {
            writer.writeString("Bucket", bucket);
            writer.writeString("Prefix", prefix);
        }

        public boolean isComplete ()
        {
            return bucket != null;
        }
    }

    public static class DistributionConfig
        extends WriteableComplexType<DistributionConfig>
    {
        public String origin;
        public String callerReference;
        public String cname;
        public String comment;
        public Boolean enabled;
        public String defaultRootObject;
        public Logging logging;
        public String originAccessIdentity;
        public boolean selfIsSigner;
        public List<String> trustedAwsSigners = Lists.newArrayList();
        public List<String> requiredProtocols = Lists.newArrayList();

        public boolean parseNextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str; Boolean bool;

            if (null != (str = reader.maybeString("Origin"))) {
                origin = str;
            } else if (null != (str = reader.maybeString("CallerReference"))) {
                callerReference = str;
            } else if (null != (str = reader.maybeString("CNAME"))) {
                cname = str;
            } else if (null != (str = reader.maybeString("Comment"))) {
                comment = str;
            } else if (null != (bool = reader.maybeBoolean("Enabled"))) {
                enabled = bool;
            } else if (null != (str = reader.maybeString("DefaultRootObject"))) {
                defaultRootObject = str;
            } else if (null != (str = reader.maybeString("OriginAccessIdentity"))) {
                originAccessIdentity = str;
            } else if (reader.peekForElement("Logging")) {
                logging = new Logging().initialize(reader);

            } else if (reader.peekForElement("TrustedSigners")) {
                new ContainerElement() {
                    public boolean parseNextElement (CloudfrontEventReader reader) throws XMLStreamException {
                        String str;
                        if (null != (str = reader.maybeString("Self"))) {
                            selfIsSigner = true;
                        } else if (null != (str = reader.maybeString("AwsAccountNumber"))) {
                            trustedAwsSigners.add(str);
                        } else {
                            return false;
                        }
                        return true;
                    }
                }.recurseInto(reader, "TrustedSigners");

            } else if (reader.peekForElement("RequiredProtocols")) {
                new ContainerElement() {
                    public boolean parseNextElement (CloudfrontEventReader reader) throws XMLStreamException {
                        String str;
                        if (null != (str = reader.maybeString("Protocol"))) {
                            requiredProtocols.add(str);
                            return true;
                        }
                        return false;
                    }
                }.recurseInto(reader, "RequiredProtocols");
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "DistributionConfig";
        }

        @Override
        public void writeElements (CloudfrontEventWriter writer)
            throws XMLStreamException
        {
            writer.writeString("Origin", origin);
            writer.writeString("CallerReference", callerReference);
            writer.writeString("CNAME", cname);
            writer.writeString("Comment", comment);
            writer.writeBoolean("Enabled", enabled);
            writer.writeString("DefaultRootObject", defaultRootObject);
            writer.writeString("OriginAccessIdentity", originAccessIdentity);

            logging.constructBody(writer);

            writer.startElement("TrustedSigners");
            if (selfIsSigner) {
                writer.writeString("Self", "");
            }
            for (String number : trustedAwsSigners) {
                writer.writeString("AwsAccountNumber", number);
            }
            writer.endElement("TrustedSigners");

            writer.startElement("RequiredProtocol");
            for (String protocol : requiredProtocols) {
                writer.writeString("Protocol", protocol);
            }
        }

        public boolean isComplete ()
        {
            return origin != null && callerReference != null && enabled != null;
        }
    }

    public DistributionAPI (String keyId, String secretKey)
    {
        super(keyId, secretKey);
    }

    /**
     * Retrieve a list of {link DistributionSummary} objects.
     */
    public List<DistributionSummary> getDistributions ()
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution?Marker=value&MaxItems=value
        GetMethod method = new GetMethod(API.DISTRIBUTION.build());
        return execute(method, new ElementListBuilder<DistributionSummary>("DistributionList") {
            @Override protected DistributionSummary createElement () {
                return new DistributionSummary();
            }
        });
    }

    /**
     * Retrieve a detailed {@link Distribution} associated with the given distribution ID.
     */
    public Distribution getDistribution (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution));
        return execute(method, new Distribution());
    }

    /**
     * Retrieve the {@link DistributionConfig} associated with the given distribution ID.
     */
    public DistributionConfig getDistributionConfig (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/config
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution, "config"));
        return execute(method, new DistributionConfig());
    }

    /**
     * Create a new distribution as specified, returning a {@link Distribution} result.
     */
    public Distribution postConfig (DistributionConfig config)
        throws CloudfrontException
    {
        // POST /2010-08-01/distribution
        return execute(new PostMethod(API.DISTRIBUTION.build()), config, new Distribution());
    }

    public Distribution putConfig (String distribution, DistributionConfig config)
        throws CloudfrontException
    {
        // PUT /2010-08-01/distribution/DistID/config
        return execute(
            new PutMethod(API.DISTRIBUTION.build(distribution, "config")),
            config, new Distribution());
    }

    public void deleteDistribution (String distribution, String tag)
        throws CloudfrontException
    {
        // DELETE /2010-08-01/distribution/DistID
        DeleteMethod method = new DeleteMethod(API.ORIGIN_ACCESS_ID.build(distribution));
        method.addRequestHeader("If-Match", tag);
        execute(method, null);
    }
}
