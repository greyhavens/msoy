//
// $Id$

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.threerings.msoy.spam.server.SpamLogic.Bucket;
import com.threerings.panopticon.aggregator.writable.Keys.LongKey;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.efs.storev2.EventWriter;

/**
 * Processes retention mail events from the advent of user buckets onwards. Subclasses provide
 * the list of buckets that we are interested in. Only aggregations of these buckets are stored.
 */
public abstract class RetentionEmailBucketed extends RetentionEmail
{
    /**
     * Create a new aggregator that only output data from the given list of buckets.
     */
    public RetentionEmailBucketed (List<Bucket> buckets)
    {
        super(new int[] { 2009, Calendar.MAY, 1 },
            new int[] { 2100, Calendar.JANUARY, 1 }, Lists.transform(buckets, TO_NAME));
    }

    @Override // from JavaAggregator
    public void write (EventWriter writer, EventDataBuilder unused, LongKey key)
        throws IOException
    {
        // the standard way of writing out the events. some subclasses may need to override this
        SubjectLineOutputBuilder builder = new SubjectLineOutputBuilder();
        builder.build(key, getSubjectLines());
        builder.write(writer);
    }

    /**
     * Gets an array of subject lines that we should output based on our bucket filter.
     */
    protected String[] getSubjectLines ()
    {
        Set<String> subjectLines = Sets.newHashSet();
        for (String bucketName : _buckets) {
            for (Bucket b : Bucket.values()) {
                if (b.name.equals(bucketName)) {
                    for (String subj : b.subjectLines) {
                        subjectLines.add(subj);
                    }
                }
            }
        }
        return subjectLines.toArray(new String[subjectLines.size()]);
    }

    /**
     * Converts a bucket into its name.
     */
    protected static final Function<Bucket, String> TO_NAME = new Function<Bucket, String> () {
        public String apply (Bucket b) {
            return b.name;
        }
    };
}
