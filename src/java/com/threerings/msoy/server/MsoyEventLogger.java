//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.logging.EventLogger;
import com.threerings.panopticon.client.logging.EventLoggerMonitor;
import com.threerings.panopticon.client.logging.NullLogger;

import com.threerings.msoy.Log;

/**
 * Wrapper around EventLogger, providing Whirled-specific typesafe logging functions.
 *
 * All logging requests will be scheduled on the invoker thread, to avoid tying up
 * the rest of the server.
 */
public class MsoyEventLogger implements EventLoggerMonitor
{
    public MsoyEventLogger (URL serverURL)
    {
        Log.log.info("Events will be logged to " + serverURL);
        _serverURL = serverURL;
    }

    /** Event: registered user logged in. */
    public void playerLoggedIn (int playerId, boolean firstLogin, String sessionToken)
    {
        post("Login_test", now(), playerId, firstLogin, sessionToken);
    }

    /** Event: guest user logged in. */
    public void guestLoggedIn (String sessionToken)
    {
        playerLoggedIn(-1, false, sessionToken);
    }

    /** Event: sent mail from one user to another. */
    public void mailSent (int fromId, int toId, int payloadType)
    {
        post("Mail_test", now(), fromId, toId, payloadType);
    }

    // from interface EventLogger.StatusMonitor
    public void afterConnect ()
    {
        // always redeclare schemas after every reconnect
        declareMsoySchemas();
    }
    
    // from interface EventLogger.StatusMonitor
    public void beforeDisconnect ()
    {
        // no op
    }

    /** Wraps a logging action in a work unit, and posts it on the queue. */
    protected void post (final String table, final Object ... values)
    {
        /* disabled during testing
        Log.log.info("Posting a log entry [table=" + table + ", values=" +
                     StringUtil.toString(values) + "].");
        MsoyServer.invoker.postUnit(new Invoker.Unit () {
            public boolean invoke () {
                logHelper(table, values);
                return false;
            }
        });
        */
    }

    /** Ensures a logger exists, and logs the event. */
    protected void logHelper (final String table, final Object [] values)
    {
        if (_logger == null) {
            _logger = new NullLogger("com.threerings.msoy", this);
            //_logger = new ServerLogger("com.threerings.msoy", _serverURL, this);
        }
        _logger.log(table, values);
    }

    /** Convenience function to return a boxed value for current time
     *  (in ms since beginning of epoch in GMT). */
    protected Long now () {
        return (Long) (new Date()).getTime();
    }
    
    protected void declareMsoySchemas ()
    {
        _logger.declareSchema(
            "Login_test",
            new String[] { "timestamp", "playerId",    "firstLogin",  "sessionToken" },
            new Class[]  { Long.class,  Integer.class, Boolean.class, String.class   });

        _logger.declareSchema(
            "Mail_test",
            new String[] { "timestamp", "senderId",    "recipientId", "payloadId" },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class });
        
        Log.log.info("Schemas declared, ready to go!");
    }

    protected EventLogger _logger;
    protected URL _serverURL;
}
