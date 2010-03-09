// $Id: $

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Date;

import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.key.field.DayKey;
import com.threerings.panopticon.aggregator.result.Result;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.eventstore.EventWriter;

@Aggregator(output=DailyVisitorFutures.OUTPUT_EVENT_NAME)
public class DailyVisitorFutures
    implements JavaAggregator<DailyVisitorFutures.EmbedDayKey>
{
    public static final String OUTPUT_EVENT_NAME = "DailyVisitorFutures";

    public static class EmbedDayKey extends DayKey
    {
        public boolean isEmbed;

        @Override public void init (EventData data)
        {
            super.init(data);

            isEmbed = data.getBoolean("embed");
        }

        @Override protected Date getDate (EventData data)
        {
            return data.getDate("acct_start");
        }
    }

    @Result(inputs=DailyAllGuestBehavior.class)
    public static class CountTypes extends FieldAggregatedResult<EmbedDayKey>
    {
        public int lost;
        public int played;
        public int converted;
        public int returned;
        public int retained;

        @Override
        public void doInit (EmbedDayKey key, EventData eventData)
        {
            retained = eventData.getInt("retained");
            returned = eventData.getInt("returned");
            converted = eventData.getInt("converted");
            played = eventData.getInt("played");
            lost = eventData.getInt("lost");
        }
    }

    public CountTypes result;

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, EmbedDayKey key)
        throws IOException
    {
        writer.write(builder.create(
            "date", key.timestamp,
            "group", (key.isEmbed ? "embed" : "web"),
            "lost", result.lost,
            "played", result.played,
            "converted", result.converted,
            "returned", result.returned,
            "retained", result.retained));
    }
}
