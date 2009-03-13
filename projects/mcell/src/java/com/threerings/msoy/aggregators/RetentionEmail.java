//
// $Id$

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.efs.storev2.StorageStrategy;
import com.threerings.panopticon.reporter.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.reporter.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.reporter.aggregator.key.NullKey.NullWritable;

import com.threerings.msoy.aggregators.result.RetentionEmailLoginsResult;
import com.threerings.msoy.aggregators.result.RetentionEmailResult;

@Aggregator(outputs="msoy.RetentionEmail", schedule="DAILY")
public class RetentionEmail
    implements JavaAggregator<NullWritable>
{
    // Our results
    public RetentionEmailResult mailings;
    public RetentionEmailLoginsResult logins;

    public static boolean isRecentEnough(EventData eventData, long days)
    {
        // hmm, we have to assume this task won't run across midnight? ...whatever
        long limit = startOfDay(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L);
        return dayOfEvent(eventData) >= limit;
    }

    public static long dayOfEvent (EventData eventData)
    {
        return startOfDay(eventData.getDate("timestamp").getTime());
    }

    public static long startOfDay (long timeInMillis)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public Class<NullWritable> getKeyClass ()
    {
        return NullWritable.class;
    }

    @Override
    public void write (EventWriter writer, NullWritable key)
        throws IOException
    {
        // output an event for each date
        for (Entry<Long, List<Integer>> dateEntry : mailings.sent.entrySet()) {
            // count the respondents
            int respondents = 0;
            for (Integer memberId : dateEntry.getValue()) {
                Long lastLogin = logins.logins.get(memberId);
                if (lastLogin != null && lastLogin >= dateEntry.getKey()) {
                    respondents++;
                }
            }
            writer.write(new EventData("msoy.RetentionEmailResponse",
                new ImmutableMap.Builder<String, Object>().put("respondents", respondents)
                .put("mailings", dateEntry.getValue().size()).build(),
                new HashMap<String, Object>()), StorageStrategy.PROCESSED);
        }
    }
}
