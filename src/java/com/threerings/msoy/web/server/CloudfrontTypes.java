//
// $Id: $

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.server.CloudfrontConnection.ElementIterable;
import com.threerings.msoy.web.server.CloudfrontConnection.ContainerElement;
import com.threerings.msoy.web.server.CloudfrontConnection.CloudfrontComplexType;

public abstract class CloudfrontTypes
{
    public static class OriginAccessIdentitySummary
        extends CloudfrontComplexType<OriginAccessIdentitySummary>
    {
        public String id;
        public String s3CanonicalUserId;
        public String comment;

        public boolean nextElement (CloudfrontEventReader reader)
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
        extends CloudfrontComplexType<OriginAccessIdentity>
    {
        public String id;
        public String s3CanonicalUserId;
        public OriginAccessIdentityConfig config;

        public boolean nextElement (CloudfrontEventReader reader)
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
        extends CloudfrontComplexType<OriginAccessIdentityConfig>
    {
        public String callerReference;
        public String comment;

        public boolean nextElement (CloudfrontEventReader reader)
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

        public boolean isComplete ()
        {
            return callerReference != null;
        }
    }

    public static class DistributionSummary
        extends CloudfrontComplexType<DistributionSummary>
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

        public boolean nextElement (CloudfrontEventReader reader)
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
                    public boolean nextElement (CloudfrontEventReader reader) throws XMLStreamException {
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
        extends CloudfrontComplexType<Signer>
    {
        public boolean isSelf;
        public String awsAccountNumber;
        public Set<String> keyIds = Sets.newHashSet();

        public boolean nextElement (CloudfrontEventReader reader)
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
        extends CloudfrontComplexType<Distribution>
    {
        public String id;
        public String status;
        public Integer inProgressInvalidationBatches;
        public Date lastModifiedTime;
        public String domainName;
        public List<Signer> activeTrustedSigners = Lists.newArrayList();
        public DistributionConfig config;

        public boolean nextElement (CloudfrontEventReader reader)
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
                    public boolean nextElement (CloudfrontEventReader reader) throws XMLStreamException {
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
        extends CloudfrontComplexType<Logging>
    {
        public String bucket;
        public String prefix;

        public boolean nextElement (CloudfrontEventReader reader)
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

        public boolean isComplete ()
        {
            return bucket != null;
        }
    }

    public static class DistributionConfig
        extends CloudfrontComplexType<DistributionConfig>
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

        public boolean nextElement (CloudfrontEventReader reader)
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
                    public boolean nextElement (CloudfrontEventReader reader) throws XMLStreamException {
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
                    public boolean nextElement (CloudfrontEventReader reader) throws XMLStreamException {
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

        public boolean isComplete ()
        {
            return origin != null && callerReference != null && enabled != null;
        }
    }

    public static class InvalidationSummary
        extends CloudfrontComplexType<InvalidationSummary>
    {
        public String id;
        public String status;

        public boolean nextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("Status"))) {
                status = str;
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "InvalidationSummary";
        }

        public boolean isComplete ()
        {
            return id != null && status != null;
        }
    }

    public static class InvalidationBatch
        extends CloudfrontComplexType<InvalidationBatch>
    {
        public String callerReference;
        public Set<String> paths = Sets.newHashSet();

        public boolean nextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str;
            if (null != (str = reader.maybeString("Path"))) {
                paths.add(str);
            } else if (null != (str = reader.maybeString("CallerReference"))) {
                callerReference = str;
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "InvalidationBatch";
        }

        public boolean isComplete ()
        {
            return callerReference != null && !paths.isEmpty();
        }
    }

    public static class Invalidation
        extends CloudfrontComplexType<Invalidation>
    {
        public String id;
        public String status;
        public Date createTime;
        public InvalidationBatch batch;

        public boolean nextElement (CloudfrontEventReader reader)
            throws XMLStreamException
        {
            String str; Date date;

            if (null != (str = reader.maybeString("Id"))) {
                id = str;
            } else if (null != (str = reader.maybeString("Status"))) {
                status = str;
            } else if (null != (date = reader.maybeDate("CreateTime"))) {
                createTime = date;
            } else if (reader.peekForElement("InvalidationBatch")) {
                batch = new InvalidationBatch().initialize(reader);
            } else {
                return false;
            }
            return true;
        }

        public String typeElement ()
        {
            return "Invalidation";
        }

        public boolean isComplete ()
        {
            return id != null && status != null && createTime != null && batch != null;
        }
    }


}
