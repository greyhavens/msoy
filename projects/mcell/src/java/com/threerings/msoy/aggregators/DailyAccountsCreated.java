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

import com.threerings.panopticon.eventstore.EventWriter;
import com.threerings.panopticon.aggregator.util.PartialDate;

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
                day = PartialDate.DAY.roundDown(date.getTime()).getTime();
            } else {
                day = null;
            }
        }
    }

    @Result(inputs=AccountsWithVectors.class)
    public static class CountTypes extends FieldAggregatedResult<DayKey>
    {
        public int total;
        public int affiliated;
        public int fromAd;
        public int organic;
        public int fromFacebookAd;
        public int facebookAffiliated;

        @Override
        public void doInit (DayKey key, EventData eventData)
        {
            total = 1;
            String vector = eventData.getString("vector");
            boolean facebook = vector.startsWith("fb.");
            if (eventData.getInt("affiliateId") > 0) {
                if (facebook) {
                    facebookAffiliated = 1;
                } else {
                    affiliated = 1;
                }

            } else if (vector.startsWith("a.")) {
                fromAd = 1;

            } else if (facebook) {
                // sort of temporary... we want to track facebook ads but there is/was no special
                // ad vector, so assume non-affiliated facebook vectors are ads
                fromFacebookAd = 1;

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
                types.affiliated, "fromAd", types.fromAd, "organic", types.organic,
                "facebookAd", types.fromFacebookAd,
                "facebookAffiliated", types.facebookAffiliated));
        }
    }

    protected Date _midnight = PartialDate.DAY.roundDown(System.currentTimeMillis()).getTime();
}
