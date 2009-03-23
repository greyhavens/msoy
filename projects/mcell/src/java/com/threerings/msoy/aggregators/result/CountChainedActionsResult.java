// $Id: CountChainedActionsResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.PropertiesAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

/**
 * Loads up sets of staged actions
 * Also brings in data from the AccountCreated table, and outputs it as conversion.
 *
 * Required config options:
 * <ul>
 *      <li><b>actionRegex</b>: Regular expression to match actions to be aggregated.</li>
 *      <li><b>maxDelay</b>: Max delay between chain elements in milliseconds.
 *          Elements that exceed this delay will count as different chains.</li>
 * </ul>
 *
 * Optional config options:
 * <ul>
 *      <li><b>timestampField</b>: Name of the timestamp field, defaults to "timestamp".</li>
 * </ul>
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class CountChainedActionsResult implements PropertiesAggregatedResult<CountChainedActionsResult>
{
    public void configure (Configuration config)
    {
        timestampField = config.getString("timestampField", "timestamp");
        actionRegex = config.getString("actionRegex");
        maxDelay = config.getLong("maxDelay");
    }

    public void combine (final CountChainedActionsResult other)
    {
        this.maxDelay = other.maxDelay;
        this.actions.putAll(other.actions);

        if (this.tracker == null) {
            this.tracker = other.tracker;
        } else {
            Preconditions.checkArgument(this.tracker.equals(other.tracker));
        }
    }

    public boolean init (final EventData eventData)
    {
        this.tracker = String.valueOf(eventData.getData().get("tracker"));
        Preconditions.checkState(maxDelay != 0 && actionRegex != null);

        // get the client action name
        final Object o = eventData.getData().get("actionName");
        if (o == null) {
            return false;
        }

        // match it against our pattern
        final String action = o.toString();
        if (!Pattern.matches(actionRegex, action)) {
            return false;
        }

        // pull the timestamp
        final Object t = eventData.getData().get(timestampField);
        if (! (t instanceof Date || t instanceof Number)) {
            return false;
        }

        // pull out action details
        final String details = String.valueOf(eventData.getData().get("details"));

        // convert timestamp to ms
        long timestamp;
        if (t instanceof Date) {
            timestamp = ((Date) t).getTime();
        } else {
            timestamp = ((Number) t).longValue();
        }

        // save
        this.actions.put(timestamp, details);
        return true;
    }

    public boolean putData (final Map<String, Object> data)
    {
        if (chains == null) {
            this.chains = new PartitionedChain();
        }

        if (! chains.iter.hasNext()) {
            return false;
        }

        data.put("actions", chains.iter.next());
        data.put("count", 1);

        return chains.iter.hasNext();
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        this.maxDelay = in.readLong();

        @SuppressWarnings("unchecked")
        Map<Long, String> otherActions = (Map<Long, String>) HadoopSerializationUtil.readObject(in);

        this.actions.clear();
        this.actions.putAll(otherActions);

        this.tracker = in.readUTF();
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeLong(this.maxDelay);
        HadoopSerializationUtil.writeObject(out, this.actions);

        out.writeUTF(this.tracker);
    }

    @Override
    public String toString()
    {
        return String.format("[CountChainedActionResults - actions: %s]", this.actions.values());
    }

    /**
     * Maintains state between putData invocations.
     */
    private class PartitionedChain
    {
        public final String separator = " > ";
        public final Iterator<String> iter;

        public PartitionedChain () {
            List<String> subchains = Lists.newArrayList();

            Iterator<Map.Entry<Long, String>> iter = actions.entrySet().iterator();
            if (! iter.hasNext()) {
                subchains.add("none");
                this.iter = subchains.iterator();
                return;
            }

            // prime the pump
            Map.Entry<Long, String> firstAction = iter.next();
            long lastTimestamp = firstAction.getKey();
            String currentSubchain = firstAction.getValue();

            // now go through and assemble subchains
            while (iter.hasNext()) {
                Map.Entry<Long, String> currentAction = iter.next();
                long currentTimestamp = currentAction.getKey();

                if (currentTimestamp > (lastTimestamp + maxDelay)) {
                    // split the chain
                    subchains.add(currentSubchain);
                    currentSubchain = currentAction.getValue();

                } else {
                    // add to the current subchain
                    currentSubchain = currentSubchain + separator + currentAction.getValue();
                }
                lastTimestamp = currentTimestamp;
            }

            subchains.add(currentSubchain);
            this.iter = subchains.iterator();
        }
    };

    /** Max delay */
    private long maxDelay;

    /** Map from timestamps to actions. */
    private final Map<Long, String> actions = Maps.newTreeMap();

    private String tracker;

    private PartitionedChain chains = null;

    private String timestampField, actionRegex;
}
