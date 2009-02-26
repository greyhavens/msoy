// $Id: WeeklyGamesNameTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.msoy.aggregators.util.WhirledInfoProvider;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.PropertiesResultTransformer;

public class WeeklyGamesNameTransformer
    implements PropertiesResultTransformer
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        // configure the singleton whirled data provider
        if (provider == null) {
            final Object host = config.getProperty("server");
            if (host != null) {
                provider = new WhirledInfoProvider(host.toString());
            }
        }
    }

    public boolean transform(EventData data)
    {
        final WhirledInfoProvider provider = getWhirledInfo();
        if (provider == null) {
            return true;      // configuration failed
        }

        // now get the name
        int gameId = Math.abs(((Number)data.getData().get("gameId")).intValue());
        WhirledInfoProvider.Info info = provider.get(WhirledInfoProvider.DataType.GAME, gameId);
        if (info == null) {
            return true;      // fetch failed
        }

        final String gameName = info.getString("name");
        if (gameName != null) {
            data.getData().put("name", gameName);
        }

        return true;
    }

    public static WhirledInfoProvider getWhirledInfo () {
        return provider;
    }

    private static WhirledInfoProvider provider;
}
