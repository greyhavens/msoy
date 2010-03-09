// $Id: $

package com.threerings.msoy.aggregators;

import java.io.IOException;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.key.field.DayKey;
import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.eventstore.EventWriter;

/**
 * Simply average the bar/coins exchange rate over each server. The exchange rate is no longer
 * fluid, and has in fact been fixed for something like a year, but we retain this task to view
 * the historical data.
  */
@Aggregator(output=DailyExchangeRate.OUTPUT_EVENT_NAME, incremental="date")
public class DailyExchangeRate
    implements JavaAggregator<DayKey>
{
    public static final String OUTPUT_EVENT_NAME = "DailyExchangeRate";

    @StringInputNameResult(inputs="ExchangeRate", incrementals="timestamp")
    public static class Accumulation extends FieldAggregatedResult<DayKey>
    {
        public double total;
        public long count;

        @Override
        public void doInit (DayKey key, EventData eventData)
        {
            total = eventData.getDouble("rate");
            count = 1;
        }
    }

    public Accumulation result;

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, DayKey key)
        throws IOException
    {
        writer.write(builder.create(
            "date", key.timestamp, "rate", result.total / result.count));
    }
}
