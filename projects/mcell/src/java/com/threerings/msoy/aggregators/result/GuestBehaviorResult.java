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
import java.util.HashMap;
import java.util.Iterator;
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
 * Acceptable input includes the following event types: ReferralCreated, AccountCreated, GameExit,
 * RoomExit.
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class GuestBehaviorResult
    implements AggregatedResult<GuestBehaviorResult>
{
    public boolean init (final EventData eventData)
    {
        final Map<String, Object> data = eventData.getData();

        // all records must have timestamp and tracker (also called visitorId in the past)
        final GuestEntry entry = new GuestEntry();
        final Date timestamp = (Date)data.get("timestamp");

        String tracker = (String)data.get("tracker");
        if (tracker == null) {
            tracker = (String)data.get("visitorId");
        }
        if (tracker == null || timestamp == null) {
            return false;
        }

        // what we do depends on the event type
        final EventName name = eventData.getEventName();

        if (VISITOR_INFO_CREATED.equals(name)) {
            entry.start = timestamp;
            Boolean web = (Boolean)data.get("web");
            if (web != null) {
                entry.embed = !web;
            }

        } else if (VECTOR_ASSOCIATED.equals(name)) {
            entry.vector = (String)data.get("vector");

        } else if (ACCOUNT_CREATED.equals(name)) {
            entry.conversion = timestamp;
            entry.conversionMember = (Integer)data.get("newMemberId");

        } else if (EXPERIENCE.equals(name)) {
            final String action = (String)data.get("action");
            if (action == null) {
                return false;
            }
            entry.addEvent(action, timestamp);

            final Integer memberId = (Integer)data.get("memberId");
            if (memberId != null && memberId > 0) {
                if (entry.firstExperienceAsPlayer == null
                    || timestamp.before(entry.firstExperienceAsPlayer)) {
                    entry.firstExperienceAsPlayer = timestamp;
                    entry.conversionMember = memberId;
                }
            }
        }

        entry.tracker = tracker;
        this.histories.put(tracker, entry);

        return true;
    }

    public void combine (final GuestBehaviorResult other)
    {
        for (Map.Entry<String, GuestEntry> entry : other.histories.entrySet()) {
            String tracker = entry.getKey();
            if (histories.containsKey(tracker)) {
                histories.get(tracker).combine(entry.getValue());
            } else {
                histories.put(tracker, entry.getValue());
            }
        }
    }

    public boolean putData (Map<String, Object> result)
    {
        if (historiter == null) {
            historiter = histories.entrySet().iterator();
        }

        // did we run out of items?
        if (!historiter.hasNext()) {
            return false;
        }

        Entry<String, GuestEntry> tuple = historiter.next();
        GuestEntry e = tuple.getValue();

        // does this entry correspond to an actual new user visit?
        if (!e.isSufficient()) {
            return false;
        }

        result.put("acct_tracker", e.tracker);

        // data from VisitorInfoCreated
        result.put("acct_start", e.start);

        // data from VectorAssociated
        String vector = e.vector;
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
            vector = String.format("(no vector - from %s)", e.embedString());
        }
        result.put("acct_vector", vector);
        result.put("acct_vector_from_ad", vector.startsWith("a."));

        // data from AccountCreated
        final Date conversion = e.getConversion();
        result.put("conv", conversion != null ? 1 : 0); // 1 if someone converted, 0 otherwise

        // data from Experiences
        final Map<String, Integer> conversionEvents = e.countByConversion(conversion,
            CheckType.CONVERSION_PERIOD);
        final Map<String, Integer> retentionWeekEvents = e.countByConversion(conversion,
            CheckType.RETENTION_WEEK);

        result.put("events_till_conversion", toCounts(conversionEvents));
        result.put("events_till_retention", toCounts(retentionWeekEvents));

        result.put("features_till_conversion", toFeatures(conversionEvents));
        result.put("features_till_retention", toFeatures(retentionWeekEvents));

        // data from both
        int minutes = 0;
        if (e.start != null && conversion != null) {
            result.put("conv_timestamp", conversion);
            minutes = (int)(conversion.getTime() - e.start.getTime()) / (1000 * 60);
        } else {
            result.put("conv_timestamp", new Date(0L));
        }
        
        if (e.conversionMember != null) {
            result.put("conv_member", e.conversionMember);
        }

        result.put("conv_minutes", minutes);
        result.put("conv_hours", minutes / 60);
        result.put("conv_days", minutes / (60 * 24));

        // pull out retention stats, as days and weeks from joining.
        // retention counts only if they have a valid start date, a valid conversion
        // date, and they had some experiences more than a week after converting.
        Date lastEventDate = e.findLastEventDate();
        boolean cameBack = (e.start != null) && (conversion != null) && (lastEventDate != null);

        int days = 0;
        if (cameBack) {
            long msecs = (lastEventDate.getTime() - e.start.getTime());
            days = (int)(msecs / (1000 * 60 * 60 * 24));
        }

        boolean retained = (days / 7) > 0;
        result.put("ret_days", days);
        result.put("ret_weeks", days / 7);
        result.put("ret", retained ? 1 : 0); // 1 if someone was retained for a week+

        // pull out conversion / retention status
        String status = "1. did not convert";
        if (conversion != null) {
            status = !retained ? "2. converted" : "3. retained";
        }
        result.put("conv_status", status);

        return historiter.hasNext();
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        histories.clear();
        final int count = in.readInt();
        for (int ii = 0; ii < count; ii++) {
            GuestEntry entry = new GuestEntry();
            entry.read(in);
            histories.put(entry.tracker, entry);
        }
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeInt(histories.size());
        for (final Entry<String, GuestEntry> entry : histories.entrySet()) {
            entry.getValue().write(out);
        }
    }

    /**
     * Produces a string mapping from experiences to counts. NOTE: this output format is consumed
     * by {@link GuestBehaviorSplitterTransformer}.
     */
    private static String toCounts (Map<String, Integer> experienceCounts)
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
    private static String toFeatures (Map<String, Integer> experienceCounts)
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

    private enum CheckType {
        CONVERSION_PERIOD, RETENTION_WEEK;
    }

    private class GuestEntry
    {
        public String vector;
        public String tracker;

        public Date start;
        public Date conversion;
        public Integer conversionMember;
        public Boolean embed;

        public Multimap<String, Date> events = Multimaps.newArrayListMultimap();
        public Date firstExperienceAsPlayer;

        public GuestEntry ()
        {
        // don't initialize anything
        }

        /**
         * Returns true if the event contains enough information to be processed as a new user
         * bounce information, ie. it needs at least the start date.
         */
        public boolean isSufficient ()
        {
            return this.start != null;
        }

        /**
         * Returns the conversion date from AccountCreated, or the date of the first experience
         * with a player's memberId, whichever is first. Returns null if neither is available.
         */
        public Date getConversion ()
        {
            if (conversion != null) {
                return conversion;
            }
            if (firstExperienceAsPlayer != null) {
                return firstExperienceAsPlayer;
            }

            return null;
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
            if (this.tracker == null) {
                this.tracker = other.tracker;
            }

            final Boolean otherExperienceEarlier = other.firstExperienceAsPlayer != null
                && this.firstExperienceAsPlayer != null
                && other.firstExperienceAsPlayer.before(this.firstExperienceAsPlayer);

            if (this.firstExperienceAsPlayer == null || otherExperienceEarlier) {
                this.firstExperienceAsPlayer = other.firstExperienceAsPlayer;
            }

            if (this.vector == null) {
                this.vector = other.vector;
            }

            if (this.embed == null) {
                this.embed = other.embed;
            }

            if (this.start == null) {
                this.start = other.start;
            }

            if (this.conversion == null) {
                this.conversion = other.conversion;
            }

            if (this.conversionMember == null) {
                this.conversionMember = other.conversionMember;
            }

            for (Map.Entry<String, Collection<Date>> entry : other.events.asMap().entrySet()) {
                for (Date date : entry.getValue()) {
                    addEvent(entry.getKey(), date);
                }
            }
        }

        public void write (final DataOutput out)
            throws IOException
        {
            HadoopSerializationUtil.writeObject(out, tracker);
            // data from VectorAssociated
            HadoopSerializationUtil.writeObject(out, vector);
            HadoopSerializationUtil.writeObject(out, embed);
            // data from VisitorInfoCreated
            HadoopSerializationUtil.writeObject(out, start);
            // data from AccountCreated
            HadoopSerializationUtil.writeObject(out, conversion);
            HadoopSerializationUtil.writeObject(out, conversionMember);
                       // data from Experiences
            HadoopSerializationUtil.writeObject(out, events);
            HadoopSerializationUtil.writeObject(out, firstExperienceAsPlayer);
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
            this.start = (Date)HadoopSerializationUtil.readObject(in);
            // data from AccountCreated
            this.conversion = (Date)HadoopSerializationUtil.readObject(in);
            this.conversionMember = (Integer)HadoopSerializationUtil.readObject(in);
                        // more data from Experiences
            this.events = (Multimap<String, Date>)HadoopSerializationUtil.readObject(in);
            this.firstExperienceAsPlayer = (Date)HadoopSerializationUtil.readObject(in);
        }
    }

    private final static EventName VISITOR_INFO_CREATED = new EventName("VisitorInfoCreated");
    private final static EventName VECTOR_ASSOCIATED = new EventName("VectorAssociated");
    private final static EventName ACCOUNT_CREATED = new EventName("AccountCreated");
    private final static EventName EXPERIENCE = new EventName("Experience");

    private final static int MAX_VECTOR_LENGTH = 40;

    private final Map<String, GuestEntry> histories = new HashMap<String, GuestEntry>();

    // iterator over histories - only used by putData();
    private Iterator<Map.Entry<String, GuestEntry>> historiter = null;
}
