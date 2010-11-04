//
// $Id: $


package com.threerings.msoy.web.server;

import javax.xml.stream.XMLStreamException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 */
public class InvalidationAPI extends CloudfrontConnection
{

    public static class InvalidationSummary
        extends ComplexType<InvalidationSummary>
    {
        public String id;
        public String status;

        public boolean parseNextElement (CloudfrontEventReader reader)
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
        extends WriteableComplexType<InvalidationBatch>
    {
        public String callerReference;
        public Set<String> paths = Sets.newHashSet();

        public boolean parseNextElement (CloudfrontEventReader reader)
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

        @Override
        public void writeElements (CloudfrontEventWriter writer) throws XMLStreamException
        {
            writer.writeString("CallerReference", callerReference);
            for (String key : paths) {
                writer.writeString("Path", key);
            }
        }
    }

    public static class Invalidation
        extends ComplexType<Invalidation>
    {
        public String id;
        public String status;
        public Date createTime;
        public InvalidationBatch batch;

        public boolean parseNextElement (CloudfrontEventReader reader)
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

    public InvalidationAPI (String keyId, String secretKey)
    {
        super(keyId, secretKey);
    }

    public List<InvalidationSummary> getInvalidations (String distribution)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/invalidation?Marker=value&MaxItems=value
        GetMethod method = new GetMethod(API.DISTRIBUTION.build(distribution, "invalidation"));
        return execute(method, new ElementListBuilder<InvalidationSummary>("InvalidationList") {
            protected InvalidationSummary createElement () {
                return new InvalidationSummary();
            }
        });
    }

    public Invalidation getInvalidation (String distribution, String batch)
        throws CloudfrontException
    {
        // GET /2010-08-01/distribution/DistID/invalidation/invalidationID
        GetMethod method = new GetMethod(
            API.DISTRIBUTION.build(distribution, "invalidation", batch));
        return execute(method, new Invalidation());
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
    public Invalidation invalidateObjects (String distribution, final Iterable<String> keys)
        throws CloudfrontException
    {
        InvalidationBatch batch = new InvalidationBatch();
        batch.callerReference = String.valueOf(System.nanoTime());
        batch.paths = Sets.newHashSet(keys);
        return postBatch(distribution, batch);
    }

    public Invalidation postBatch (String distribution, InvalidationBatch batch)
        throws CloudfrontException
    {
        // POST /2010-08-01/distribution/DistID/invalidation
        return execute(
            new PostMethod(API.DISTRIBUTION.build(distribution, "invalidation")),
            batch, new Invalidation());
    }
}
