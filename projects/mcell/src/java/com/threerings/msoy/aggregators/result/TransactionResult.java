// $Id: TransactionResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.io.WritableComparable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.PropertiesAggregatedResult;
import com.threerings.panopticon.aggregator.util.Util.AdditiveIntMap;
import com.threerings.panopticon.common.event.EventData;

/**
 * Adds up transactions across all currencies.
 */
public class TransactionResult
    implements PropertiesAggregatedResult<TransactionResult>
{
    public void configure (Configuration config)
    {
        for (CurrencyAccumulator accum : accumulators) {
            accum.configure(config);
        }
    }

    private static class CurrencyAccumulator
    {
        public int earned, spent;
        public AdditiveIntMap<String> details = new AdditiveIntMap<String>();

        public String deltaFieldName;
        public String detailsConfigField;
        public String longPrefix;
        public String shortPrefix;

        public CurrencyAccumulator (String deltaFieldName, String details, String longPrefix,
                String shortPrefix)
        {
            this.deltaFieldName = deltaFieldName;
            this.detailsConfigField = details;
            this.longPrefix = longPrefix;
            this.shortPrefix = shortPrefix;
        }

        public void configure (Configuration config)
        {
            keyvals = config.getStringArray(detailsConfigField);
        }

        public void combine (CurrencyAccumulator other)
        {
            Preconditions.checkArgument(other.deltaFieldName.equals(this.deltaFieldName));
            // assume that if one field matches, all fields match

            this.earned += other.earned;
            this.spent += other.spent;

            for (Entry<String, Integer> entry : other.details.entrySet()) {
                this.details.add(entry.getKey(), entry.getValue());
            }
        }

        public void processEvent (EventData eventData)
        {
            Map<String, String> actions = Maps.newHashMap(); // map from action # to action name

            // use the config file to initialize our details table
            for (String keyval : keyvals) {
                String[] temp = keyval.split(":");
                actions.put(temp[0], temp[1]);
                this.details.add(temp[1], 0);
            }

            // does this event have the right data?
            Object obj = eventData.getData().get(deltaFieldName);
            if (obj == null) {
                return;
            }

            // does it have a non-zero value for the currency we care about?
            int value = ((Number)obj).intValue();
            if (value == 0) {
                return;
            }

            // store as appropriate
            if (value > 0) {
                earned += value;
            } else {
                spent -= value;
            }

            // store details, if desired
            Object actionObject = eventData.getData().get("actionType");
            if (actionObject == null) {
                return;
            }

            String actionNumber = actions.get(actionObject.toString());
            if (actionNumber == null) {
                return; // we don't care about the details of this action
            }

            this.details.add(actionNumber, value);
        }

        public void putData (Map<String, Object> result)
        {
            result.put(longPrefix + "earned", this.earned);
            result.put(longPrefix + "spent", this.spent);

            double percent = (earned == 0) ? 0 : ((double) spent) / earned;
            result.put(longPrefix + "spending_p", percent);

            for (Entry<String, Integer> detail : this.details.entrySet()) {
                result.put(shortPrefix + detail.getKey(), detail.getValue());
            }
        }

        @SuppressWarnings("unchecked")
        public void read (DataInput in)
            throws IOException
        {
            this.earned = in.readInt();
            this.spent = in.readInt();
            this.details = (AdditiveIntMap<String>)HadoopSerializationUtil.readObject(in);
        }

        public void write (DataOutput out)
            throws IOException
        {
            out.writeInt(this.earned);
            out.writeInt(this.spent);
            HadoopSerializationUtil.writeObject(out, this.details);
        }

        private String[] keyvals;
    }

    private final CurrencyAccumulator[] accumulators = {
        new CurrencyAccumulator("deltaFlow", "coinsDetails", "coins_", "c_"),
        new CurrencyAccumulator("deltaBars", "barsDetails", "bars_", "b_"),
        new CurrencyAccumulator("deltaBling", "blingDetails", "bling_", "bl_") };

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        for (CurrencyAccumulator acc : accumulators) {
            acc.processEvent(eventData);
        }

        return true;
    }

    public void combine (TransactionResult result)
    {
        for (int ii = 0; ii < this.accumulators.length; ii++) {
            this.accumulators[ii].combine(result.accumulators[ii]);
        }
    }

    public boolean putData (Map<String, Object> result)
    {
        for (CurrencyAccumulator acc : accumulators) {
            acc.putData(result);
        }

        return false;
    }

    public void readFields (DataInput in)
        throws IOException
    {
        for (CurrencyAccumulator acc : accumulators) {
            acc.read(in);
        }
    }

    public void write (DataOutput out)
        throws IOException
    {
        for (CurrencyAccumulator acc : accumulators) {
            acc.write(out);
        }
    }
}
