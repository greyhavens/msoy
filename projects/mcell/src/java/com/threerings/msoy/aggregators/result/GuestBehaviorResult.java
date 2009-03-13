// $Id: GuestBehaviorResult.java 1382 2009-02-25 03:01:12Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Join;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import com.threerings.msoy.aggregators.trans.GuestBehaviorSplitterTransformer;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;
import com.threerings.panopticon.reporter.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

/**
 * Extracts a guest behavior table, including the time they first show up, the time they register
 * (if ever), and a short trace of actions in between.
 *
 * Acceptable input includes the following event types: VisitorInfoCreated, AccountCreated, Experience,
 * VectorAssociated.
 */
public class GuestBehaviorResult
    implements AggregatedResult<GuestBehaviorResult>
{
    public boolean init (final EventData eventData)
    {
        final Map<String, Object> data = eventData.getData();

        // all records must have timestamp and tracker (also called visitorId in the past)
        _entry = new GuestEntry();
        final Date timestamp = (Date)data.get("timestamp");

        String tracker = (String)data.get("tracker");
        if (tracker == null) {
            tracker = (String)data.get("visitorId");
        }
        if (tracker == null || timestamp == null) {
            return false;
        }

        _entry.tracker = tracker;

        // what we do depends on the event type
        final EventName name = eventData.getEventName();

        if (VISITOR_INFO_CREATED.equals(name)) {
            Boolean web = (Boolean)data.get("web");
            if (web != null) {
                _entry.embed = !web;
            }
            _entry.created = timestamp;

        } else if (VECTOR_ASSOCIATED.equals(name)) {
            _entry.vector = (String)data.get("vector");

        } else if (ACCOUNT_CREATED.equals(name)) {
            Boolean isGuest = (Boolean) data.get("isGuest");
            if (isGuest != null && isGuest.booleanValue()) {
                // if they created a permaguest account, they played Whirled
                _entry.played = timestamp;
            } else {
                // if they saved their permaguest account, they're fully converted
                _entry.converted = timestamp;
            }
            _entry.member = (Integer)data.get("newMemberId");

        } else if (EXPERIENCE.equals(name)) {
            final String action = (String)data.get("action");
            if (action == null) {
                return false;
            }
            _entry.addEvent(action, timestamp);
        }

        return true;
    }

    public void combine (final GuestBehaviorResult other)
    {
        if (_entry == null) {
            _entry = other.getEntry();
        } else {
            _entry.combine(other.getEntry());
        }
    }

    public boolean putData (Map<String, Object> result)
    {
        // There are two ways a tracker get created: either a VisitorInfoCreated is generated,
        // which will result in _entry.created being non-null, or we went straight to creating
        // a permaguest, in which case an AccountCreated with isGuest = true will have been sent
        // over the wire, which in turn will cause _entry.played to be non-null.
        
        Date created;
        if (_entry.created != null) {
            created = _entry.created;
            
        } else if (_entry.played != null) {
            created = _entry.played;
            
        } else {
            // otherwise this is a tracker that did stuff within the past 30 days, but which
            // was created earlier than that: we simply drop it
            return false;
        }

        result.put("acct_start", created);
        result.put("acct_tracker", _entry.tracker);

        // data from VectorAssociated
        String vector = _entry.vector;
        if (vector != null) {
            int ampersand = vector.indexOf("&");
            if (ampersand > -1) {
                // remove any parameters that got passed in accidentally
                vector = vector.substring(0, ampersand);
            }
            if (vector.length() > MAX_VECTOR_LENGTH) {
                vector = vector.substring(0, MAX_VECTOR_LENGTH) + "...";
            }
        }
        if (vector == null || "".equals(vector)) {
            vector = String.format("(no vector - from %s)", _entry.embedString());
        }
        result.put("acct_vector", vector);
        result.put("acct_vector_from_ad", vector.startsWith("a."));

        result.put("conv", _entry.converted != null ? 1 : 0);
        result.put("played", (_entry.converted != null || _entry.played != null) ? 1 : 0);

        // data from Experiences
        final Map<String, Integer> conversionEvents =
            _entry.countByConversion(_entry.converted, CheckType.CONVERSION_PERIOD);
        final Map<String, Integer> retentionWeekEvents =
            _entry.countByConversion(_entry.converted, CheckType.RETENTION_WEEK);

        result.put("events_till_conversion", toCounts(conversionEvents));
        result.put("events_till_retention", toCounts(retentionWeekEvents));

        result.put("features_till_conversion", toFeatures(conversionEvents));
        result.put("features_till_retention", toFeatures(retentionWeekEvents));

        // data from both
        int minutes = 0;
        if (_entry.converted != null) {
            result.put("conv_timestamp", _entry.converted);
            minutes = (int)(_entry.converted.getTime() - created.getTime()) / (1000 * 60);
        } else {
            result.put("conv_timestamp", new Date(0L));
        }
        
        if (_entry.member != null) {
            result.put("conv_member", _entry.member);
        } else {
            result.put("conv_member", Integer.valueOf(0));
        }

        result.put("conv_minutes", minutes);
        result.put("conv_hours", minutes / 60);
        result.put("conv_days", minutes / (60 * 24));

        // pull out retention stats, as days and weeks from joining.
        // retention counts only if they have a valid start date, a valid conversion
        // date, and they had some experiences more than a week after converting.
        Date lastEventDate = _entry.findLastEventDate();
        int days = 0;
        if (_entry.converted != null && lastEventDate != null) {
            long msecs = (lastEventDate.getTime() - _entry.created.getTime());
            days = (int)(msecs / (1000 * 60 * 60 * 24));
        }

        boolean retained = (days / 7) > 0;
        result.put("ret_days", days);
        result.put("ret_weeks", days / 7);
        result.put("ret", retained ? 1 : 0); // 1 if someone was retained for a week+

        // pull out conversion / retention status
        String status = "1. did not convert";
        if (_entry.converted != null) {
            status = !retained ? "2. converted" : "3. retained";
        }
        result.put("conv_status", status);

        return false;
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        if ((Boolean)HadoopSerializationUtil.readObject(in)) {
            _entry = new GuestEntry();
            _entry.read(in);
        }
    }
    
    public void write (final DataOutput out)
        throws IOException
    {
        if (_entry != null) {
            HadoopSerializationUtil.writeObject(out, true);
            _entry.write(out);
        } else {
            HadoopSerializationUtil.writeObject(out, false);
        }
    }

    protected GuestEntry getEntry ()
    {
        return _entry;
    }

    protected GuestEntry _entry;

    protected enum CheckType {
        CONVERSION_PERIOD, RETENTION_WEEK;
    }

    protected class GuestEntry
    {
        public String vector;
        public String tracker;
        public Date created;
        
        public Boolean embed;

        public Integer member;
        public Date converted;
        public Date played;
        
        public Multimap<String, Date> events = Multimaps.newArrayListMultimap();

        public GuestEntry ()
        {
            // don't initialize anything
        }

        /** Returns the ternary 'embed' as a string. */
        public String embedString ()
        {
            if (embed == null) {
                return "unknown";
            }
            return embed.booleanValue() ? "embed" : "web";
        }

        /** Add an event to the list of ones we're collecting for this tracking number. */
        public void addEvent (String event, Date date)
        {
            events.put(event, date);
        }

        /** Retrieves the last event date, or null for empty sets. */
        public Date findLastEventDate ()
        {
            Date last = null;
            for (Date date : events.values()) {
                if (last == null || date.after(last)) {
                    last = date;
                }
            }
            return last;
        }

        /**
         * Returns a map from experience name to the count of those experiences in the given
         * period.
         */
        public Map<String, Integer> countByConversion (final Date cc, final CheckType type)
        {
            switch(type) {
            case CONVERSION_PERIOD:
                // find all events up until conversion time (or current time, if applicable)
                final long from = 0;
                final long to = (cc != null ? cc.getTime() : Long.MAX_VALUE);
                return countByTimePeriod(from, to);

            case RETENTION_WEEK:
                if (cc == null) {
                    // someone who didn't convert can't be retained
                    return countByTimePeriod(0L, 0L);
                }

                // only return entries between conversion and conversion + 7 days
                final Calendar retention = Calendar.getInstance();
                retention.clear();
                retention.setTime(cc);
                retention.add(Calendar.DATE, 7);
                return countByTimePeriod(cc.getTime(), retention.getTimeInMillis());

            }

            return null;
        }

        /**
         * Returns a map from experience name to the count in the given period.
         */
        public Map<String, Integer> countByTimePeriod (final long from, final long to)
        {
            final TreeMap<String, Integer> results = new TreeMap<String, Integer>();
            String[] names = { "AL", "GA", "GM", "GS", "SB", "SD", "SP", "VR", "VW", "FR", "FP",
                "IU", "IL", "ER", "EP" };
            for (String experience : names) {
                Collection<Date> dates = this.events.get(experience);
                int count = 0;
                if (dates != null) {
                    for (Date date : dates) {
                        final long time = date.getTime();
                        if (time >= from && time < to) {
                            count++;
                        }
                    }
                }
                results.put(experience, count);
            }

            return results;
        }
        
        /** Fills in any null fields on this instance from the other, and adds map entries. */
        public void combine (GuestEntry other)
        {
            if (!this.tracker.equals(other.tracker)) {
                throw new RuntimeException(
                    "Won't combine entries with different keys (" + this.tracker + ", " + other.tracker + ")");
            }

            created = nullMerge(created, other.created);
            vector = nullMerge(vector, other.vector);
            embed = nullMerge(embed, other.embed);
            converted = nullMerge(converted, other.converted);
            played = nullMerge(played, other.played);
            member = nullMerge(member, other.member);

            for (Map.Entry<String, Collection<Date>> entry : other.events.asMap().entrySet()) {
                for (Date date : entry.getValue()) {
                    addEvent(entry.getKey(), date);
                }
            }
        }
        
        protected <T> T nullMerge(T ours, T theirs) {
            return (ours != null) ? ours : theirs;
        }
        
        public void write (final DataOutput out)
            throws IOException
        {
            HadoopSerializationUtil.writeObject(out, tracker);
            // data from VectorAssociated
            HadoopSerializationUtil.writeObject(out, vector);
            HadoopSerializationUtil.writeObject(out, embed);
            // data from VisitorInfoCreated
            HadoopSerializationUtil.writeObject(out, created);
            // data from AccountCreated
            HadoopSerializationUtil.writeObject(out, converted);
            HadoopSerializationUtil.writeObject(out, played);
            HadoopSerializationUtil.writeObject(out, member);
            // data from Experiences
            HadoopSerializationUtil.writeObject(out, events);
        }

        @SuppressWarnings("unchecked")
        public void read (final DataInput in)
            throws IOException
        {
            this.tracker = (String)HadoopSerializationUtil.readObject(in);
            // data from VectorAssociated
            this.vector = (String)HadoopSerializationUtil.readObject(in);
            this.embed = (Boolean)HadoopSerializationUtil.readObject(in);
            // data from VisitorInfoCreated
            this.created = (Date)HadoopSerializationUtil.readObject(in);
            // data from AccountCreated
            this.converted = (Date)HadoopSerializationUtil.readObject(in);
            this.played = (Date)HadoopSerializationUtil.readObject(in);
            this.member = (Integer)HadoopSerializationUtil.readObject(in);
            // more data from Experiences
            this.events = (Multimap<String, Date>)HadoopSerializationUtil.readObject(in);
        }
    }

    /**
     * Produces a string mapping from experiences to counts. NOTE: this output format is consumed
     * by {@link GuestBehaviorSplitterTransformer}.
     */
    protected static String toCounts (Map<String, Integer> experienceCounts)
    {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, Integer> entry : experienceCounts.entrySet()) {
            builder.append(entry.getKey()).append(':').append(entry.getValue()).append(' ');
        }
        return builder.toString();
    }

    /**
     * Produces an experience feature vector.
     */
    protected static String toFeatures (Map<String, Integer> experienceCounts)
    {
        Iterable<String> results = Iterables.transform(experienceCounts.entrySet(),
            new Function<Map.Entry<String, Integer>, String>() {
                public String apply (Entry<String, Integer> entry)
                {
                    return (entry.getValue() != null && entry.getValue() > 0) ? entry.getKey()
                        : "--";
                }
            });
        return Join.join(" ", results);
    }

    protected final static EventName VISITOR_INFO_CREATED = new EventName("VisitorInfoCreated");
    protected final static EventName VECTOR_ASSOCIATED = new EventName("VectorAssociated");
    protected final static EventName ACCOUNT_CREATED = new EventName("AccountCreated");
    protected final static EventName EXPERIENCE = new EventName("Experience");

    protected final static int MAX_VECTOR_LENGTH = 40;
}
