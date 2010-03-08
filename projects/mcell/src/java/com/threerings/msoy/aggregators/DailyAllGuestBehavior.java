// $Id: $

package com.threerings.msoy.aggregators;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.threerings.msoy.aggregators.result.GuestBehaviorResult;
import com.threerings.msoy.aggregators.result.GuestBehaviorResult.CheckType;
import com.threerings.msoy.aggregators.trans.GuestBehaviorSplitterTransformer;
import com.threerings.panopticon.aggregator.AggregatorFilters;
import com.threerings.panopticon.aggregator.hadoop.Aggregator;
import com.threerings.panopticon.aggregator.hadoop.FilterFactory;
import com.threerings.panopticon.aggregator.hadoop.JavaAggregator;
import com.threerings.panopticon.aggregator.result.field.FieldKey;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventDataBuilder;
import com.threerings.panopticon.common.event.EventName;
import com.threerings.panopticon.eventstore.EventWriter;
import com.threerings.panopticon.eventstore.index.EventFilter;

@Aggregator(output="AllGuestBehavior", nexts={ DailyVisitorFutures.class,
    PerVectorExperiences.Conversion.class, PerVectorExperiences.Retention.class })
public class DailyAllGuestBehavior
    implements FilterFactory, JavaAggregator<DailyAllGuestBehavior.TrackerKey>
{
    public static class TrackerKey extends FieldKey {
        public String tracker;

        @Override public void init (EventData eventData) {
            tracker = eventData.getString("tracker");
        }
    }

    @Override
    public EventFilter createFilter (EventName name)
    {
        return new AggregatorFilters.LastMonth();
    }

    public GuestBehaviorResult result;

    @Override
    public void write (EventWriter writer, EventDataBuilder builder, TrackerKey key)
        throws IOException
    {
        Date created;
        if (result.created != null) {
            created = result.created;

        } else if (result.played != null) {
            created = result.played;

        } else {
            // otherwise this is a tracker that did stuff within the past 30 days, but which
            // was created earlier than that: we simply drop it
            return;
        }

        // data from VectorAssociated
        String vector = result.vector;
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
            vector = String.format("(no vector - from %s)", result.embedString());
        }

        boolean played = (result.played != null);
        boolean converted = (result.converted != null);

        // data from Experiences
        final Map<String, Integer> conversionEvents =
            result.countByConversion(result.converted, CheckType.CONVERSION_PERIOD);
        final Map<String, Integer> retentionWeekEvents =
            result.countByConversion(result.converted, CheckType.RETENTION_WEEK);


        Date lastEventDate = result.findLastEventDate();

        // returning is defined as any experience > 24 hours after creation
        int returnDays = 0;
        if (lastEventDate != null) {
            long msecs = (lastEventDate.getTime() - created.getTime());
            returnDays = (int)(msecs / (1000 * 60 * 60 * 24));
        }

        boolean returned = (returnDays > 0);

        // pull out retention stats, as days and weeks from joining.
        // retention counts only if they have a valid start date, a valid conversion
        // date, and they had some experiences more than a week after converting.
        int retainDays = 0;
        if (converted && lastEventDate != null) {
            long msecs = (lastEventDate.getTime() - created.getTime());
            retainDays = (int)(msecs / (1000 * 60 * 60 * 24));
        }

        boolean retained = (retainDays / 7) > 0;

        // pull out conversion / retention status
        String status = "1. did not convert";
        if (converted) {
            status = !retained ? "2. converted" : "3. retained";
        }

        int minutes = converted ?
            (int)(result.converted.getTime() - created.getTime()) / (1000 * 60) : 0;

        writer.write(builder.create(
            "acct_start", created,
            "acct_tracker", key.tracker,
            "acct_vector", vector,
            "acct_vector_from_ad", vector.startsWith("a."),
            "embed", (result.embed != null && result.embed.booleanValue()),
            "ret_days", retainDays,
            "ret_weeks", retainDays / 7,
            "conv_status", status,

            // the status of this visitor for funnel purposes
            "lost", (!played && !converted && !returned && !retained) ? 1 : 0,
            "played", (played && !converted && !returned && !retained) ? 1 : 0,
            "converted", (converted && !returned && !retained) ? 1 : 0,
            "returned", (returned && !retained) ? 1 : 0,
            "retained", retained ? 1 : 0,

            "events_till_conversion", toCounts(conversionEvents),
            "events_till_retention", toCounts(retentionWeekEvents),
            "features_till_conversion", toFeatures(conversionEvents),
            "features_till_retention", toFeatures(retentionWeekEvents),

            "conv_member", (result.member != null) ? result.member : 0,
            "conv_timestamp", converted ? result.converted : new Date(0L),

            "conv_minutes", minutes,
            "conv_hours", minutes / 60,
            "conv_days", minutes / (60 * 24)));
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
        return Joiner.on(" ").join(results);
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


    protected final static int MAX_VECTOR_LENGTH = 40;
}
