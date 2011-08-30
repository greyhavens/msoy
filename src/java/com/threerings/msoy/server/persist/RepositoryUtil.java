//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.util.Calendars;
import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;

import com.threerings.util.PostgresUtil;

/**
 * Repository related utility methods.
 */
public class RepositoryUtil
{
    /**
     * Returns a timestamp that cuts off the specified number of days in the past at midnight
     * rather than at the current time minus that many milliseconds. Use this when using cutoffs
     * for timestamps in queries to ensure that they can be cached.
     */
    public static Timestamp getCutoff (int days)
    {
        return Calendars.now().zeroTime().addDays(-days).toTimestamp();
    }

    public static ConnectionProvider createConnectionProvider (Config config)
        throws Exception
    {
        // if we are dealing with an old-school configuration, just use the no-connection-pool bits
        String url = config.getValue("db.default.url", "");
        if (!StringUtil.isBlank(url)) {
            return new StaticConnectionProvider(config.getSubProperties("db"));
        }
        // otherwise do things using a postgres pooled data source
        return PostgresUtil.createPoolingProvider(config, "msoy");
    }
}
