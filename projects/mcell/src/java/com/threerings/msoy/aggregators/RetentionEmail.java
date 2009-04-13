//
// $Id$

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.CountHashMap;
import com.threerings.msoy.aggregators.result.RetentionEmailLoginsResult;
import com.threerings.msoy.aggregators.result.RetentionEmailResult;
import com.threerings.panopticon.aggregator.Schedule;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.hadoop.KeyFactory;
import com.threerings.panopticon.aggregator.hadoop.KeyInitData;
import com.threerings.panopticon.aggregator.writable.Keys;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.efs.storev2.EventWriter;

@Aggregator(output = RetentionEmail.OUTPUT, schedule = Schedule.DAILY)
public class RetentionEmail
    implements JavaAggregator<Keys.LongKey>, KeyFactory<Keys.LongKey>
{
    public static final String OUTPUT = "msoy.RetentionEmailResponse";
    // Our results
    public RetentionEmailResult mailings;
    public RetentionEmailLoginsResult logins;

    @Override
    public List<Keys.LongKey> createKeys (KeyInitData keyInitData)
    {
        EventData eventData = keyInitData.eventData;
        if (RetentionEmailResult.checkInputs(eventData)) {
            return Collections.singletonList(new Keys.LongKey(
                getDayOfEvent(eventData).getTimeInMillis()));

        } else if (RetentionEmailLoginsResult.checkInputs(eventData)) {
            final int daysToConsider = 14;
            final long cutoff = System.currentTimeMillis() - daysToConsider * 24 * 60 * 60 * 1000L;
            Calendar calendar = getDayOfEvent(eventData);
            List<Keys.LongKey> keys = Lists.newArrayList();
            for (int day = 0; day < daysToConsider; ++day) {
                if (calendar.getTimeInMillis() < cutoff) {
                    break;
                }
                keys.add(new Keys.LongKey(calendar.getTimeInMillis()));
                calendar.add(Calendar.DATE, -1);
            }
            return keys;
        }

        return Collections.emptyList();
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, Keys.LongKey key)
        throws IOException
    {
        // count up how many people who received an email logged back in, group by subject line
        CountHashMap<String> respondents = new CountHashMap<String>();
        for (Map.Entry<String, Set<Integer>> entry : mailings.sent.entrySet()) {
            for (int memberId : entry.getValue()) {
                int count = logins.logins.contains(memberId) ? 1 : 0;
                respondents.incrementCount(entry.getKey(), count);
            }
        }

        // create our standard output columns
        Map<String, Object> eventData = Maps.newHashMap();
        eventData.put("totalRespondents", respondents.getTotalCount());
        eventData.put("mailings", mailings.sent.size());
        eventData.put("date", new Date(key.get()));

        // and one output column per subject line
        for (CountHashMap.Entry<String> subjCount : respondents.countEntrySet()) {
            eventData.put(subjCount.getKey(), subjCount.getCount());
        }

        // write the event
        writer.write(new EventData(OUTPUT, eventData, new HashMap<String, Object>()));
    }

    protected static Calendar getDayOfEvent (EventData eventData)
    {
        Calendar calendar = Calendar.getInstance();
        Object timestamp = eventData.get("timestamp");
        calendar.setTimeInMillis(timestamp instanceof Date ?
            ((Date)timestamp).getTime() : (Long)timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
