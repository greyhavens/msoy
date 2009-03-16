//
// $Id$

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.efs.storev2.StorageStrategy;
import com.threerings.panopticon.reporter.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.reporter.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.reporter.aggregator.hadoop.KeyFactory;
import com.threerings.panopticon.reporter.aggregator.hadoop.KeyInitData;
import com.threerings.panopticon.reporter.aggregator.writable.Keys;

import com.threerings.msoy.aggregators.result.RetentionEmailLoginsResult;
import com.threerings.msoy.aggregators.result.RetentionEmailResult;

@Aggregator(outputs="msoy.RetentionEmail", schedule="DAILY")
public class RetentionEmail
    implements JavaAggregator<Keys.LongKey>, KeyFactory<Keys.LongKey>
{
    // Our results
    public RetentionEmailResult mailings;
    public RetentionEmailLoginsResult logins;

    @Override
    public Class<Keys.LongKey> getKeyClass ()
    {
        return Keys.LongKey.class;
    }

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

        return EMPTY_LIST;
    }

    @Override
    public void write (EventWriter writer, Keys.LongKey key)
        throws IOException
    {
        // count the respondents
        int respondents = 0;
        for (Integer memberId : mailings.sent) {
            if (logins.logins.contains(memberId)) {
                respondents++;
            }
        }
        // write an event
        writer.write(new EventData("msoy.RetentionEmailResponse",
            new ImmutableMap.Builder<String, Object>()
                .put("respondents", respondents)
                .put("mailings", mailings.sent.size())
                .put("date", new Date(key.get())).build(),
            new HashMap<String, Object>()), StorageStrategy.PROCESSED);
    }

    protected static Calendar getDayOfEvent (EventData eventData)
    {
        Date timestamp = eventData.getDate("timestamp");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp.getTime());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @SuppressWarnings("unchecked")
    protected static List<Keys.LongKey> EMPTY_LIST = Collections.EMPTY_LIST;
}
