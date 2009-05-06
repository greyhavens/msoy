//
// $Id$

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;

import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.writable.Keys.LongKey;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.efs.storev2.EventWriter;

/**
 * Before we had buckets, there were subject lines. Before we had subject lines, there were only
 * mailings. This handles both cases by processing "default" buckets prior to 5/1/9 and "default"
 * subject lines prior to 4/4/9. This can be removed eventually.
 */
@Aggregator(output="msoy.RetentionEmailResponse_Old")
public class RetentionEmailOld extends RetentionEmail
{
    public RetentionEmailOld ()
    {
        super(new int[] { 2009, Calendar.MARCH, 1 },
            new int[] { 2009, Calendar.APRIL, 30 }, Collections.singletonList("default"));
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
