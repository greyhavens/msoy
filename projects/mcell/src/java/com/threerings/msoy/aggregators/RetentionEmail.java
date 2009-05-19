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
import com.threerings.msoy.aggregators.result.RetentionEmailResult.Mailing;
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

    public void iterate (ResultProcessor processor)
    {
        for (Mailing mailing : mailings.sent.values()) {
            if (_buckets.contains(mailing.bucket)) {
                processor.process(mailing, logins.memberIds.contains(mailing.memberId));
            }
        }
    }

    protected static interface ResultProcessor
    {
        void process (Mailing mailing, boolean responded);
    }

    protected abstract class KeyedOutputBuilder<Key> implements ResultProcessor
    {
        Map<String, Object> eventData = Maps.newHashMap();

        /** Number of emails sent, by key. */
        public CountHashMap<Key> sent = new CountHashMap<Key>();

        /** Number of people who logged in within the DAYS_TO_CONSIDER, by key. */
        public CountHashMap<Key> respondents = new CountHashMap<Key>();

        abstract Key getColumnId (Mailing mailing);

        public void write (EventWriter writer)
            throws IOException
        {
            Map<String, Object> props = new HashMap<String, Object>();
            writer.write(new EventData(getOutputEventName(), eventData, props));
        }

        protected void build (Keys.LongKey key, Key... columnIds)
        {
            iterate(this);

            // create our standard output columns
            eventData.put("totalRespondents", respondents.getTotalCount());
            eventData.put("mailings", sent.getTotalCount());
            eventData.put("date", new Date(key.get()));

            // and 3 output columns per key
            for (Key id : columnIds) {
                // number of responses by subject line
                eventData.put(id + "Rsp", respondents.getCount(id));
                // number sent by subject line
                eventData.put(id + "Sent", sent.getCount(id));
                // response rate (%) by subject line
                int sentCount = sent.getCount(id);
                double rate = sentCount == 0 ? 0 :
                    (double)(respondents.getCount(id)) / sent.getCount(id);
                eventData.put(id + "Pct", rate * 100);
            }
        }

        @Override // from ResultIterator
        public void process (Mailing mailing, boolean responded)
        {
            sent.incrementCount(getColumnId(mailing), 1);
            if (responded) {
                respondents.incrementCount(getColumnId(mailing), 1);
            }
        }
    }

    protected class SubjectLineOutputBuilder extends KeyedOutputBuilder<String>
    {
        @Override
        public String getColumnId (Mailing mailing) {
            return mailing.subject;
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
