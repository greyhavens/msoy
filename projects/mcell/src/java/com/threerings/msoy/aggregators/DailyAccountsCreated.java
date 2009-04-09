package com.threerings.msoy.aggregators;

import java.io.IOException;

import java.util.Date;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.result.Result;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.aggregator.result.field.FieldKey;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;

import com.threerings.panopticon.efs.storev2.EventWriter;
import com.threerings.panopticon.shared.util.PartialDateType;
import com.threerings.panopticon.shared.util.TimeRange;

@Aggregator(output=DailyAccountsCreated.OUTPUT_EVENT_NAME)
public class DailyAccountsCreated
    implements JavaAggregator<DailyAccountsCreated.DayKey>
{
    public static final String OUTPUT_EVENT_NAME = "DailyAccountsCreated";

    public static class DayKey extends FieldKey
    {
        public Date day;

        @Override
        public void init (EventData data)
        {
            Date date = data.getDate("date");
            if (date != null) {
                day = TimeRange.roundDown(date.getTime(), PartialDateType.DAY).getTime();
            } else {
                day = null;
            }
        }
    }

    @Result(inputs=AccountsWithVectors.class)
    public static class CountTypes extends FieldAggregatedResult
    {
        public int total;
        public int affiliated;
        public int fromAd;
        public int organic;

        @Override
        public void doInit (EventData eventData)
        {
            total = 1;

            if (eventData.getInt("affiliateId") > 0) {
                affiliated = 1;
            } else if (eventData.getString("vector").startsWith("a.")) {
                fromAd = 1;
            } else {
                organic = 1;
            }
        }

    }

    public CountTypes types;

    public void write (EventWriter writer, EventDataBuilder builder, DayKey key)
        throws IOException
    {
        if (key.day.before(_midnight)) {
            writer.write(builder.create("date", key.day, "total", types.total, "affiliated",
                types.affiliated, "fromAd", types.fromAd, "organic", types.organic));
        }
    }

    protected Date _midnight =
        TimeRange.roundDown(System.currentTimeMillis(), PartialDateType.DAY).getTime();
}
