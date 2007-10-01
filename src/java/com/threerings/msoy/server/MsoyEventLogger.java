//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.net.MalformedURLException;

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
 * Logging functions are safe for concurrent access from different threads. All logging requests
 * will be scheduled on the invoker thread, to serialize them, and to avoid tying up
 * the rest of the server.
 */
public class MsoyEventLogger
{
    /** Initializes the logger; this must happen before any events can be logged. */
    public MsoyEventLogger (final URL serverURL)
    {
        Log.log.info("Events will be logged to " + serverURL);
        
        // _storage = new EchoStorage();
        _storage = new NullStorage();
        // _storage = new ServerStorage(serverURL);
        
        _logger = new EventLogger("com.threerings.msoy", _storage, MSOY_SCHEMAS);
    }

    /** Event: flow transation. */
    public void flowTransaction (
        int playerId, int actionType, int flowDelta, int newTotal, String details)
    {
        post("FlowTransaction_test", now(), playerId, actionType, flowDelta, newTotal, details);
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
        post("MailSent_test", now(), fromId, toId, payloadType);
    }

    /** Event: added a friend. */
    public void friendAdded (int playerId, int friendId)
    {
        post("FriendListAction_test", now(), playerId, friendId, true);
    }

    /** Event: removed a friend. */
    public void friendRemoved (int playerId, int friendId)
    {
        post("FriendListAction_test", now(), playerId, friendId, false);
    }

    /** Event: joined a group. */
    public void groupJoined (int playerId, int groupId)
    {
        post("GroupMembershipAction_test", now(), playerId, groupId, true);
    }

    /** Event: joined a group. */
    public void groupLeft (int playerId, int groupId)
    {
        post("GroupMembershipAction_test", now(), playerId, groupId, false);
    }

    /** Event: promotion/demotion in a group. */
    public void groupRankChange (int playerId, int groupId, byte newRank)
    {
        post("GroupRankModification_test", now(), playerId, groupId, newRank);
    }
    
    /** Wraps a logging action in a work unit, and posts it on the queue. */
    protected void post (final String event, final Object ... values)
    {
        if (_logger == null) {
            throw new RuntimeException("MsoyEventLogger not initialized, cannot log events.");
        } else {
            MsoyServer.invoker.postUnit(new Invoker.Unit () {
                public boolean invoke () {
                    _logger.log(event, values);
                    return false;
                }
            });
        }
    }

    /** Convenience function to return a boxed value for current time
     *  (in ms since beginning of epoch in GMT). */
    protected Long now () {
        return System.currentTimeMillis();
    }
    
    protected static final EventSchema[] MSOY_SCHEMAS = new EventSchema[] {
        new EventSchema(
            "Login_test",
            new String[] { "timestamp", "playerId",    "firstLogin",  "sessionToken" },
            new Class[]  { Long.class,  Integer.class, Boolean.class, String.class   }),
        new EventSchema(
            "MailSent_test",
            new String[] { "timestamp", "senderId",    "recipientId", "payloadId"    },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class  }),
        new EventSchema(
            "FlowTransaction_test",
            new String[] { "timestamp", "playerId",    "actionType",  "flowDelta",
                              "newTotal",    "details" },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class,
                           Integer.class, String.class }),
        new EventSchema(
            "FriendListAction_test",
            new String[] { "timestamp", "playerId",    "friendId",    "isAdded"      },
            new Class[]  { Long.class,  Integer.class, Integer.class, Boolean.class  }),
        new EventSchema(
            "GroupMembershipAction_test",
            new String[] { "timestamp", "playerId",    "groupId",     "isJoined"     },
            new Class[]  { Long.class,   Integer.class, Integer.class, Boolean.class  }),
        new EventSchema(
            "GroupRankModification_test",
            new String[] { "timestamp", "playerId",   "groupId",     "newRank"        },
            new Class[]  { Long.class,  Integer.class, Integer.class, Byte.class       })

    };        

    /** Singleton reference to log storage. */
    protected LogStorage _storage;

    /** Singleton reference to the event logger instance. */
    protected EventLogger _logger;
}
