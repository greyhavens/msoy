//
// $Id$

package com.threerings.msoy.server;

import java.net.URL;
import java.net.MalformedURLException;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import com.threerings.panopticon.data.Schema;
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
        
        //_storage = new EchoStorage();
        _storage = new NullStorage();
        // _storage = new ServerStorage(serverURL);
        
        _logger = new EventLogger(_storage, MSOY_SCHEMAS);
    }

    /** Event: periodic system snapshot of player counts. */
    public void currentPlayerStats (String serverName, int total, int active, int guests)
    {
        post("CurrentPlayerStats_test", now(), serverName, total, active, guests);
    }
    
    /** Event: generic flow transaction. */
    public void flowTransaction (
        int playerId, int actionType, int flowDelta, int newTotal, String details)
    {
        post("FlowTransaction_test", now(), playerId, actionType, flowDelta, newTotal, details);
    }

    /** Event: item purchase. */
    public void itemPurchased (
        int playerId, byte itemType, int itemId, int flowCost, int goldCost)
    {
        post("ItemPurchase_test", now(), playerId, itemType, itemId, flowCost, goldCost);
    }
    
    /** Event: item purchase. */
    public void itemListedInCatalog (
        int creatorId, byte itemType, int itemId, int flowCost, int goldCost,
        int pricing, int salesTarget)
    {
        post("ItemCatalogListing_test", now(), creatorId, itemType, itemId,
             flowCost, goldCost, pricing, salesTarget);
    }

    /** Event: registered user authenticated with the specified cookie. */
    public void userAuthenticated (int playerId, boolean firstLogin, String sessionToken)
    {
        post("Login_test", now(), false, playerId, firstLogin, sessionToken);
    }

    /** Event: guest user logged in with the specified cookie. */
    public void userAuthenticated (String sessionToken)
    {
        post("Login_test", now(), true, -1, false, sessionToken);
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
                    _logger.log(MSOY, event, values);
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

    protected static final String MSOY = "com.threerings.msoy";
    protected static final Schema[] MSOY_SCHEMAS = new Schema[] {
        new Schema(
            MSOY, "CurrentPlayerStats_test",
            new String[] { "timestamp", "serverName", "total",       "active",
                           "guests" },
            new Class[]  { Long.class,   String.class, Integer.class, Integer.class,
                           Integer.class }),
        new Schema(
            MSOY, "Login_test",
            new String[] { "timestamp", "guest",       "playerId",    "firstLogin",
                           "sessionToken" },
            new Class[]  { Long.class,   Boolean.class, Integer.class, Boolean.class,
                           String.class }),
        new Schema(
            MSOY, "MailSent_test",
            new String[] { "timestamp", "senderId",   "recipientId", "payloadId"     },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class  }),
        new Schema(
            MSOY, "FlowTransaction_test",
            new String[] { "timestamp", "playerId",   "actionType",  "flowDelta",
                              "newTotal",    "details"     },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class,
                               Integer.class, String.class }),
        new Schema(
            MSOY, "ItemPurchase_test",
            new String[] { "timestamp", "playerId",   "itemType", "itemId",
                           "flowCost",    "goldCost"     },
            new Class[]  { Long.class,  Integer.class, Integer.class, Integer.class,
                            Integer.class, Integer.class }),
        new Schema(
            MSOY, "ItemCatalogListing_test",
            new String[] { "timestamp", "creatorId",   "itemType",    "itemId",
                           "flowCost",    "goldCost",    "pricing",     "salesTarget"  },
            new Class[]  { Long.class,   Integer.class, Integer.class, Integer.class,
                           Integer.class,  Integer.class, Integer.class, Integer.class }),
        new Schema(
            MSOY, "FriendListAction_test",
            new String[] { "timestamp", "playerId",   "friendId",    "isAdded"       },
            new Class[]  { Long.class,  Integer.class, Integer.class, Boolean.class  }),
        new Schema(
            MSOY, "GroupMembershipAction_test",
            new String[] { "timestamp", "playerId",    "groupId",     "isJoined"     },
            new Class[]  { Long.class,   Integer.class, Integer.class, Boolean.class }),
        new Schema(
            MSOY, "GroupRankModification_test",
            new String[] { "timestamp", "playerId",    "groupId",     "newRank"      },
            new Class[]  { Long.class,   Integer.class, Integer.class, Integer.class    })
    };        

    /** Singleton reference to log storage. */
    protected LogStorage _storage;

    /** Singleton reference to the event logger instance. */
    protected EventLogger _logger;
}
