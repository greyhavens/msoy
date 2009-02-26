// $Id: GuestBehaviorSplitterTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.google.common.base.Preconditions;

import com.threerings.msoy.aggregators.result.GuestBehaviorResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.PropertiesResultTransformer;

/**
 * Converts the long "events" string produced by {@link GuestBehaviorResult}
 * into a set of separate columns.
 *
 * Required config options:
 * <ul>
 *      <li><b>sourceField</b>: name of the field to be split.</li>
 * </ul>
 */
public class GuestBehaviorSplitterTransformer
    implements PropertiesResultTransformer
{
    public void configure(final Configuration config) throws ConfigurationException
    {
        this.sourceField = config.getString("sourceField");
        if (this.sourceField == null) {
            throw new ConfigurationException(
                "Aggregator transform property 'sourceField' is required.");
        }
    }

    public boolean transform (EventData data)
    {
        Preconditions.checkNotNull(sourceField);

        String field = (String) data.get(sourceField);
        if (field == null) {
            return false; // bail if we don't have this field
        }

        splitAndPopulate(data, field);
        return true;
    }

    /** Splits up the field, and populates the data map with the results. */
    public static void splitAndPopulate (EventData data, String field)
    {
        // split me up
        String[] entries = field.split(" ");
        for (String entry : entries) {
            String[] tokens = entry.split(":");
            String key = tokens[0];
            Object value = tokens[1];
            try {
                value = Integer.parseInt(tokens[1]); // try parsing if possible
            } catch (Exception e) {
                // no op - just fall through and use the last known value of 'value'
            }

            data.getData().put(key, value);
        }
    }

    private String sourceField;
}
