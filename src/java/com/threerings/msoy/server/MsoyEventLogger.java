//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.net.MalformedURLException;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.logging.EventLogger;
import com.threerings.msoy.Log;

/**
 * Shim on EventLogger, providing Whirled-specific typesafe logging functions.
 *
 * All logging requests will be scheduled on the invoker thread, to avoid tying up
 * the rest of the server.
 */
public class MsoyEventLogger extends EventLogger
{
    public MsoyEventLogger (URL serverURL)
    {
        super("com.threerings.msoy", serverURL);

        Log.log.info("Events will be logged to " + serverURL);
    }

    @Override // from EventLogger
    public void connect ()
    {
        super.connect();

        // for testing only
        Log.log.info("EventLogger connect.");

        // every time we reconnect, make sure to redeclare our schemas
        if (isConnected()) {
            declareMsoySchemas();
        }
    }
    
    @Override // from EventLogger
    public void declareSchema (String table, String[] names, Class[] types)
        throws IllegalArgumentException
    {
        super.declareSchema(table, names, types);

        // for testing only
        Log.log.info("EventLogger define schema [table=" + table + ", names=" +
                     StringUtil.toString(names) + ", types=" + StringUtil.toString(types) + "].");
    }

    @Override // from EventLogger
    public void log (String table, Object ... values)
        throws IllegalArgumentException
    {
        super.log(table, values);

        // for testing only
        Log.log.info("EventLogger processing [table=" + table + ", values=" +
                     StringUtil.toString(values) + "].");
    }

    /** User login action, msoy-specific. */
    public void logLogin (int playerId)
    {
        post("Login_test", 0, playerId);
    }


    /** Wraps a logging action in a work unit, and posts it on the queue. */
    protected void post (final String table, final Object ... values)
    {
        MsoyServer.invoker.postUnit(new Invoker.Unit () {
            public boolean invoke () {
                log(table, values);
                return false;
            }
        });
    }
    
    protected void declareMsoySchemas ()
    {
        declareSchema("Login_test",
                      new String[] { "timestamp", "playerId" },
                      new Class[]  { Long.class,  Integer.class });
        
        Log.log.info("Schemas declared, ready to go!");
    }

    protected EventLogger _logger;
}
