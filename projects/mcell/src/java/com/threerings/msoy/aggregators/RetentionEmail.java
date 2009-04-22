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
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.hadoop.KeyFactory;
import com.threerings.panopticon.aggregator.hadoop.KeyInitData;
import com.threerings.panopticon.aggregator.writable.Keys;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.shared.util.DateFactory;

@Aggregator(output = RetentionEmail.OUTPUT)
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
            final long daysToConsider = 21;
            Calendar calendar = getDayOfEvent(eventData);
            List<Keys.LongKey> keys = Lists.newArrayList();
            for (int day = 0; day < daysToConsider; ++day) {
                if (calendar.getTimeInMillis() < _inceptionTime) {
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
        // count up how many people who were sent an email logged back in, group by subject line
        CountHashMap<String> sent = new CountHashMap<String>();
        CountHashMap<String> respondents = new CountHashMap<String>();
        for (Map.Entry<String, Set<Integer>> entry : mailings.sent.entrySet()) {
            sent.incrementCount(entry.getKey(), entry.getValue().size());
            for (int memberId : entry.getValue()) {
                int count = logins.memberIds.contains(memberId) ? 1 : 0;
                respondents.incrementCount(entry.getKey(), count);
            }
        }

        // create our standard output columns
        Map<String, Object> eventData = Maps.newHashMap();
        eventData.put("totalRespondents", respondents.getTotalCount());
        eventData.put("mailings", sent.getTotalCount());
        eventData.put("date", new Date(key.get()));

        // and 3 output columns per subject line
        for (String subjLine : _subjectLines) {
            // number of responses by subject line
            eventData.put(subjLine + "Rsp", respondents.getCount(subjLine));
            // number sent by subject line
            eventData.put(subjLine + "Sent", sent.getCount(subjLine));
            // response rate (%) by subject line
            float rate = (float)(respondents.getCount(subjLine)) / sent.getCount(subjLine);
            eventData.put(subjLine + "Pct", (int)(rate * 100));
        }

        // write the event
        writer.write(new EventData(OUTPUT, eventData, new HashMap<String, Object>()));
    }

    protected static Calendar getDayOfEvent (EventData eventData)
    {
        Calendar calendar = DateFactory.newCalendar();
        Object timestamp = eventData.get("timestamp");
        calendar.setTimeInMillis(timestamp instanceof Date ?
            ((Date)timestamp).getTime() : (Long)timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /** Lower bound for earliest retention mailing event. */
    protected static final long _inceptionTime;

    // ideally, we would not need to hardwire these, but every instance of the output event needs
    // to have the same data fields
    protected static final String[] _subjectLines = {
        "nameNewThings", "nameBusyFriends", "whirledFeedAndNewThings", "default"};

    static
    {
        Calendar cal = DateFactory.newCalendar();
        cal.set(2009, 3, 1);
        _inceptionTime = cal.getTimeInMillis();
    }
}
