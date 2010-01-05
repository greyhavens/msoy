package com.threerings.msoy.aggregators;

import java.io.IOException;

import java.text.DecimalFormat;
import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.msoy.aggregators.result.GuestExperienceResult;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.result.field.FieldKey;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;

import com.threerings.panopticon.eventstore.EventWriter;

public abstract class PerVectorExperiences
    implements JavaAggregator<PerVectorExperiences.VectorConversionKey>
{
    @Aggregator(output="VectorGuestConversionExperiences")
    public static class Conversion extends PerVectorExperiences {
        public GuestExperienceResult.Conversion result;
        @Override protected GuestExperienceResult getResult () {
            return result;
        }
    }

    @Aggregator(output="VectorGuestRetentionExperiences")
    public static class Retention extends PerVectorExperiences {
        public GuestExperienceResult.Retention result;
        @Override protected GuestExperienceResult getResult () {
            return result;
        }
    }

    public static class VectorConversionKey extends FieldKey
    {
        public String vecCon;

        @Override
        public void init (EventData data)
        {
            vecCon = data.getString("acct_vector") + "-" + data.getString("conv_status");
        }
    }

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, VectorConversionKey key)
        throws IOException
    {
        Map<String, Object> props = Maps.newHashMap();
        GuestExperienceResult result = getResult();

        props.put("acct_vector", result.vector);
        props.put("conv_status", result.status);

        int total = result.trackers.size();
        props.put("count", total);

        for (Map.Entry<String, Integer> entry : result.participation.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
            double percentage = (total == 0) ? 0.0 : ((double) entry.getValue()) / total;
            String pstr = format.format(percentage);
            props.put("p_" + entry.getKey(), pstr);
        }

        writer.write(builder.create(props));
    }

    protected abstract GuestExperienceResult getResult ();

    private static final DecimalFormat format = new DecimalFormat("#,##0.00%");
}
