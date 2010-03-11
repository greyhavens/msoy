package com.threerings.msoy.aggregators;

import java.io.IOException;

import java.util.Date;
import java.util.Set;

import com.google.common.collect.Sets;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.key.field.DayKey;
import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.common.event.EventName;

import com.threerings.panopticon.eventstore.EventWriter;
import com.threerings.panopticon.aggregator.util.PartialDate;

@Aggregator(output=DailyLoginCount.OUTPUT_EVENT_NAME, incremental="timestamp")
public class DailyLoginCount
    implements JavaAggregator<DailyLoginCount.EmbedDayKey>
{
    public static final String OUTPUT_EVENT_NAME = "DailyLoginCount";

    public static class EmbedDayKey extends DayKey {
        public boolean isEmbedded;

        @Override public void init (EventData eventData) {
            super.init(eventData);

            // the only way we have of knowing whether or not a login is through an embed
            // is to trust the vector :/
            this.isEmbedded = eventData.getDefaultString("vector", "").startsWith("e.");
        }
    }

    @StringInputNameResult(inputs={"Login", "VisitorInfoCreated"}, incrementals={"timestamp"})
    public static class CountLogins extends FieldAggregatedResult<EmbedDayKey>
    {
        public Set<String> uniqueVisitors = Sets.newHashSet();
        public Set<Integer> uniquePlayers = Sets.newHashSet();
        public Set<Integer> uniqueGuests = Sets.newHashSet();

        @Override
        public void doInit (EmbedDayKey key, EventData eventData)
        {
            EventName name = eventData.getEventName();

            // register trackers both from LOGIN and VISITOR_INFO_CREATED events, coping with
            // old events where tracker was called sessionToken
            if (eventData.containsKey("tracker")) {
                uniqueVisitors.add((String)eventData.get("tracker"));
            } else if (eventData.containsKey("sessionToken")) {
                uniqueVisitors.add((String)eventData.get("sessionToken"));
            }

            // the real juice is in the LOGIN event though
            if (LOGIN.equals(name)) {
                int memberId = eventData.getInt("memberId");
                if (eventData.getDefaultBoolean("isGuest", false)) {
                    uniqueGuests.add(memberId);
                } else {
                    uniquePlayers.add(memberId);
                }
            }
        }
    }

    public CountLogins logins;

    public void write (EventWriter writer, EventDataBuilder builder, EmbedDayKey key)
        throws IOException
    {
        if (key.timestamp.before(_midnight)) {
            writer.write(builder.create("timestamp", key.timestamp,
                "embed", key.isEmbedded,
                "uniqueVisitors", logins.uniqueVisitors.size(),
                "uniquePlayers", logins.uniquePlayers.size(),
                "totalPlayers", Sets.union(logins.uniquePlayers, logins.uniqueGuests).size(),
                "uniqueGuests", logins.uniqueGuests.size()));
        }
    }

    protected final static EventName LOGIN = new EventName("Login");
    protected Date _midnight = PartialDate.DAY.roundDown(System.currentTimeMillis()).getTime();
}
