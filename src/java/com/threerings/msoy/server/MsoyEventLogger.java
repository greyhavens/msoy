//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.panopticon.client.logging.EventSchema;
import com.threerings.panopticon.client.logging.EventLogger;
import com.threerings.panopticon.client.logging.EchoStorage;
import com.threerings.panopticon.client.logging.LogStorage;
import com.threerings.panopticon.client.logging.NullStorage;
import com.threerings.panopticon.client.logging.ServerStorage;

import com.threerings.msoy.Log;

/**
 * Wrapper around EventLogger, providing Whirled-specific typesafe logging functions.
 *
 * All logging requests will be scheduled on the invoker thread, to avoid tying up
 * the rest of the server.
 */
public class MsoyEventLogger
{
    public MsoyEventLogger (URL serverURL)
    {
        Log.log.info("Events will be logged to " + serverURL);

        _serverURL = serverURL;
        _storage = new NullStorage();
        // _storage = new EchoStorage();
        // _storage = new ServerStorage(serverURL);
        _logger = new EventLogger("com.threerings.msoy", _storage, MSOY_SCHEMAS);
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

    /** Wraps a logging action in a work unit, and posts it on the queue. */
    protected void post (final String event, final Object ... values)
    {
        MsoyServer.invoker.postUnit(new Invoker.Unit () {
            public boolean invoke () {
                _logger.log(event, values);
                return false;
            }
        });
    }

    /** Ensures a logger exists, and logs the event. */
    protected void logHelper (final String table, final Object [] values)
    {

        Log.log.info("Posted a log entry [table=" + table + ", values=" +
                     StringUtil.toString(values) + "].");
    }

    /** Convenience function to return a boxed value for current time
     *  (in ms since beginning of epoch in GMT). */
    protected Long now () {
        return (Long) (new Date()).getTime();
    }
    
    protected static final EventSchema[] MSOY_SCHEMAS = new EventSchema[] {
        new EventSchema(
            "Login_test",
            new String[] { "timestamp", "playerId",    "firstLogin",  "sessionToken" },
            new Class[]  { Long.class,  Integer.class, Boolean.class, String.class   }),
        new EventSchema(
            "Mail_test",
            new String[] { "timestamp", "senderId",    "recipientId", "payloadId"    },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class  })
    };        

    protected LogStorage _storage;
    protected EventLogger _logger;
    protected URL _serverURL;
}
