// $Id: TruncateToIntervalTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.panopticon.aggregator.PropertiesResultTransformer;
import com.threerings.panopticon.common.event.EventData;

/**
 * Removes data with the 'embed' field set or not set. This could be a much more general
 * purpose class, but since we're phrase out property-based aggregators it doesn't really
 * matter.
 *     <li><b>keepEmbedded</b>: true to keep embedded events, false to keep non-embedded.
 * </ul>
 */
public class EmbedFilter
    implements PropertiesResultTransformer
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        _keepEmbedded = config.getBoolean("keepEmbedded");
    }

    public boolean transform (EventData data)
    {
        return data.getBoolean("embed") == _keepEmbedded;
    }

    protected boolean _keepEmbedded;
}
