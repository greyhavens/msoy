package com.threerings.msoy.aggregators;

import java.io.IOException;

import com.google.common.collect.Lists;
import com.threerings.msoy.aggregators.result.RetentionEmailResult.Mailing;
import com.threerings.msoy.spam.server.SpamLogic.Bucket;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.writable.Keys.LongKey;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.eventstore.EventWriter;

@Aggregator(output="msoy.RetentionEmailResponse.Validation")
public class RetentionEmailValidation extends RetentionEmailBucketed
{
    public RetentionEmailValidation ()
    {
        // process all buckets
        super(Lists.newArrayList(Bucket.values()));
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder unused, LongKey key)
        throws IOException
    {
        ValidatedOutputBuilder bldr = new ValidatedOutputBuilder();
        bldr.build(key, Boolean.TRUE, Boolean.FALSE);
        bldr.write(writer);
    }

    protected class ValidatedOutputBuilder extends KeyedOutputBuilder<Boolean>
    {
        @Override
        public Boolean getColumnId (Mailing mailing) {
            return mailing.validated;
        }
    }
}
