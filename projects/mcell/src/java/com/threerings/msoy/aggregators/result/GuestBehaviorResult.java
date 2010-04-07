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
import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;
import com.threerings.panopticon.common.util.DateFactory;

/**
 * Extracts a guest behavior table, including the time they first show up, the time they register
 * (if ever), and a short trace of actions in between.
 *
 * Acceptable input includes the following event types: VisitorInfoCreated, AccountCreated, Experience,
 * VectorAssociated.
 */
@StringInputNameResult(inputs={"VisitorInfoCreated", "VectorAssociated",
    "AccountCreated", "Experience"})
public class GuestBehaviorResult
    extends FieldResult<WritableComparable<?>, GuestBehaviorResult>
{
    public enum CheckType {
        CONVERSION_PERIOD, RETENTION_WEEK;
    }

    public String vector;
    public Date created;
    public Boolean embed;
    public Integer member;
    public Date converted;
    public Date played;

    public Multimap<String, Date> events = ArrayListMultimap.create();

    @Override
    public boolean shouldInit (WritableComparable<?> key, EventData data)
    {
        return data.containsKey("tracker") && data.containsKey("timestamp");
    }

    @Override
    public void doInit (WritableComparable<?> key, EventData data)
    {
        Date timestamp = data.getDate("timestamp");

        // what we do depends on the event type
        EventName name = data.getEventName();

        if (VISITOR_INFO_CREATED.equals(name)) {
            this.embed = !data.getDefaultBoolean("web", true);
            this.created = timestamp;

        } else if (VECTOR_ASSOCIATED.equals(name)) {
            this.vector = data.getDefaultString("vector", "???");

        } else if (ACCOUNT_CREATED.equals(name)) {
            this.played = timestamp;
            this.member = data.getDefaultInt("newMemberId", 0);
            if (!data.getDefaultBoolean("isGuest", false)) {
                this.converted = timestamp;
            }

        } else if (EXPERIENCE.equals(name)) {
            if (data.containsKey("action")) {
                this.addEvent(data.getString("action"), timestamp);
            }
        }
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
            final Calendar retention = DateFactory.newCalendar();
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
    public void combine (GuestBehaviorResult other)
    {
        created = oldest(created, other.created);
        played = oldest(played, other.played);
        converted = nullMerge(converted, other.converted);

        vector = nullMerge(vector, other.vector);
        embed = nullMerge(embed, other.embed);
        member = nullMerge(member, other.member);

        for (Map.Entry<String, Collection<Date>> entry : other.events.asMap().entrySet()) {
            for (Date date : entry.getValue()) {
                addEvent(entry.getKey(), date);
            }
        }
    }

    @Override
    public void write (final DataOutput out)
        throws IOException
    {
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

    @Override
    public void readFields (final DataInput in)
        throws IOException
    {
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
        @SuppressWarnings("unchecked")
        Multimap<String, Date> mm = (Multimap<String, Date>)HadoopSerializationUtil.readObject(in);
        this.events = mm;
    }

    protected static Date oldest (Date ours, Date theirs)
    {
        return (theirs == null || (ours != null && theirs.after(ours))) ? ours : theirs;
    }

    protected static <T> T nullMerge(T ours, T theirs) {
        return (theirs == null) ? ours : theirs;
    }

    protected final static EventName VISITOR_INFO_CREATED = new EventName("VisitorInfoCreated");
    protected final static EventName VECTOR_ASSOCIATED = new EventName("VectorAssociated");
    protected final static EventName ACCOUNT_CREATED = new EventName("AccountCreated");
    protected final static EventName EXPERIENCE = new EventName("Experience");
}
