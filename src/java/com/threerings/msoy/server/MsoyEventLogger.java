//
// $Id$

package com.threerings.msoy.server;

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
    public MsoyEventLogger (String host, int port)
    {
        super("com.threerings.msoy", host, port);
        Log.log.info("Events will be logged to " + host + ":" + port);

        connect();
    }

    @Override // from EventLogger
    public void connect ()
    {
        // todo: actually connect

        // every time we reconnect, make sure to redeclare our schemas
        declareMsoySchemas();
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
    public void log (String table, Object[] values)
        throws IllegalArgumentException
    {
        super.log(table, values);

        // for testing only
        Log.log.info("EventLogger processing [table=" + table + ", values=" +
                     StringUtil.toString(values) + "].");
    }

    protected void declareMsoySchemas ()
    {
        /*
        // for testing only
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                declareSchema("FlowTransaction",
                              new String[] { "type",       "playerId", "time" },
                              new Class[]  { String.class, int.class,  Long.class });
                return false;
            }
        });
        */
    }        
}
