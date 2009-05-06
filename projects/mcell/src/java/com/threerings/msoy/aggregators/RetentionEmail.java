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

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.CountHashMap;
import com.threerings.msoy.aggregators.result.RetentionEmailLoginsResult;
import com.threerings.msoy.aggregators.result.RetentionEmailResult;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.hadoop.KeyFactory;
import com.threerings.panopticon.aggregator.hadoop.KeyInitData;
import com.threerings.panopticon.aggregator.writable.Keys;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.shared.util.DateFactory;


public abstract class RetentionEmail
    implements JavaAggregator<Keys.LongKey>, KeyFactory<Keys.LongKey>
{
    // Our results
    public RetentionEmailResult mailings;
    public RetentionEmailLoginsResult logins;

    public RetentionEmail (int[] firstYMD, int[] lastYMD, List<String> bucketNames)
    {
        Calendar cal = DateFactory.newCalendar();
        cal.clear();

        cal.set(firstYMD[0], firstYMD[1], firstYMD[2]);
        _first = cal.getTimeInMillis();

        cal.set(lastYMD[0], lastYMD[1], lastYMD[2]);
        _last = cal.getTimeInMillis();

        _buckets = Sets.newHashSet(bucketNames);
    }

    @Override
    public List<Keys.LongKey> createKeys (KeyInitData keyInitData)
    {
        EventData eventData = keyInitData.eventData;
        if (RetentionEmailResult.checkInputs(eventData)) {
            long date = getDayOfEvent(eventData).getTimeInMillis();
            if (date < _first || date > _last) {
                return Collections.emptyList();
            }
            return Collections.singletonList(new Keys.LongKey(date));

        } else if (RetentionEmailLoginsResult.checkInputs(eventData)) {
            final long daysToConsider = 21;
            Calendar calendar = getDayOfEvent(eventData);
            List<Keys.LongKey> keys = Lists.newArrayList();
            for (int day = 0; day < daysToConsider; ++day) {
                long millis = calendar.getTimeInMillis();
                if (millis < _first) {
                    break;
                }
                if (millis <= _last) {
                    keys.add(new Keys.LongKey(millis));
                }
                calendar.add(Calendar.DATE, -1);
            }
            return keys;
        }

        return Collections.emptyList();
    }

    protected class OutputBuilder
    {
        /** Number of emails sent, by subject line. */
        public CountHashMap<String> sent = new CountHashMap<String>();

        /** Number of people who logged in within the DAYS_TO_CONSIDER, by subject line */
        public CountHashMap<String> respondents = new CountHashMap<String>();

        Map<String, Object> eventData = Maps.newHashMap();

        public void write (EventWriter writer)
            throws IOException
        {
            Map<String, Object> props = new HashMap<String, Object>();
            writer.write(new EventData(getOutputEventName(), eventData, props));
        }

        public void build (Keys.LongKey key, String[] hardCodedSubjectLine)
        {
            for (Map.Entry<String, Map<String, Set<Integer>>> entry :
                mailings.sent.entrySet()) {
                if (_buckets.contains(entry.getKey())) {
                    addBucket(entry.getKey(), entry.getValue());
                }
            }

            // create our standard output columns
            eventData.put("totalRespondents", respondents.getTotalCount());
            eventData.put("mailings", sent.getTotalCount());
            eventData.put("date", new Date(key.get()));

            // and 3 output columns per subject line
            for (String subjLine : hardCodedSubjectLine) {
                addSubjectLineTotals(subjLine);
            }
        }

        public void addBucket (String bucket, Map<String, Set<Integer>> subjectLines)
        {
            for (Map.Entry<String, Set<Integer>> entry : subjectLines.entrySet()) {
                addRecipients(entry.getKey(), entry.getValue());
            }
        }

        public void addRecipients (String subjectLine, Set<Integer> recipients)
        {
            sent.incrementCount(subjectLine, recipients.size());
            for (int memberId : recipients) {
                addRecipient(subjectLine, memberId);
            }
        }

        public void addRecipient (String subjectLine, int memberId)
        {
            int count = logins.memberIds.contains(memberId) ? 1 : 0;
            respondents.incrementCount(subjectLine, count);
        }

        public void addSubjectLineTotals (String subjLine)
        {
            // number of responses by subject line
            eventData.put(subjLine + "Rsp", respondents.getCount(subjLine));
            // number sent by subject line
            eventData.put(subjLine + "Sent", sent.getCount(subjLine));
            // response rate (%) by subject line
            float rate = (float)(respondents.getCount(subjLine)) / sent.getCount(subjLine);
            eventData.put(subjLine + "Pct", (int)(rate * 100));
        }
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

    protected String getOutputEventName ()
    {
        return getClass().getAnnotation(Aggregator.class).output();
    }

    protected static final int DAYS_TO_CONSIDER = 21;

    protected long _first, _last;
    protected Set<String> _buckets;

    protected final static Logger log = Logger.getLogger(RetentionEmail.class);
}
