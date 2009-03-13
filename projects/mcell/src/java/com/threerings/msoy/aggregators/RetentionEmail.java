package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.Maps;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.efs.storev2.StorageStrategy;
import com.threerings.panopticon.reporter.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.reporter.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.reporter.aggregator.key.NullKey.NullWritable;

import com.threerings.msoy.aggregators.result.RetentionEmailResult;

@Aggregator(outputs="msoy.RetentionEmail", schedule="DAILY")
public class RetentionEmail
    implements JavaAggregator<NullWritable>
{
    public RetentionEmailResult result;

    @Override
    public Class<NullWritable> getKeyClass ()
    {
        return NullWritable.class;
    }

    @Override
    public void write (EventWriter writer, NullWritable key)
        throws IOException
    {
        // our output is just the result fields in this case
        Map<String, Object> data = Maps.newHashMap();
        data.put("sent", result.sent);
        data.put("counts", result.counts);

        // TODO: what goes here?
        Map<String, Object> props = Maps.newHashMap();

        // write out the new event
        writer.write(new EventData("msoy.RetentionEmail", data, props), StorageStrategy.PROCESSED);
    }
}
