//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.logging.EventLogger;
import com.threerings.msoy.Log;

/**
 * Event logger that sends individual event records to an appropriate logging server.
 *
 * <b>Note that this implementation is not synchronized.</b> Multiple threads should not access
 * an EventLogger concurrently. 
 */
public class MsoyEventLogger extends EventLogger
{
    public MsoyEventLogger (String host, int port)
    {
        super("com.threerings.msoy", host, port);
        Log.log.info("Events will be logged to " + host + ":" + port);

        if (connect()) {
            declareMsoySchemas();
        }
    }

    @Override // from EventLogger
    public void declareSchema (String table, Object ... definitions)
        throws IllegalArgumentException
    {
        super.declareSchema(table, definitions);
        // for testing only
        Log.log.info("EventLogger define schema [table=" + table +
                     ", data=" + definitions + "].");
    }

    @Override // from EventLogger
    public void log (String table, Object ... namesAndValues)
        throws IllegalArgumentException
    {
        super.log(table, namesAndValues);
        // for testing only
        Log.log.info("EventLogger processing [table=" + table +
                     ", data=" + namesAndValues + "].");
    }

    protected void declareMsoySchemas ()
    {
        declareSchema("FlowTransaction",
                      "type", int.class, "playerId", int.class, "time", long.class);
    }        
}
