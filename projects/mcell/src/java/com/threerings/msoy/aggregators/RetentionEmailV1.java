package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;

import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.writable.Keys.LongKey;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.efs.storev2.EventWriter;

@Aggregator(output="msoy.RetentionEmailResponse")
public class RetentionEmailV1 extends RetentionEmail
{
    public RetentionEmailV1 ()
    {
        super(new int[] { 2009, Calendar.MARCH, 1 },
            new int[] { 2009, Calendar.APRIL, 30 }, new String[] { "default" });
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder unused, LongKey key)
        throws IOException
    {
        OutputBuilder builder = new OutputBuilder();
        builder.build(key, _subjectLines);
        builder.write(writer);
    }

    protected static final String[] _subjectLines = {
        "nameNewThings", "nameBusyFriends", "whirledFeedAndNewThings", "default"};
}
