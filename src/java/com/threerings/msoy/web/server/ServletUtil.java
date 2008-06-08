//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.PopularPlacesSnapshot;
import com.threerings.msoy.server.persist.MemberCardRecord;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.FriendFeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;
import com.threerings.msoy.person.data.SelfFeedMessage;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;
import com.threerings.msoy.person.server.persist.SelfFeedMessageRecord;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.PlaceCard;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains utility methods used by servlets.
 */
public class ServletUtil
{
    /**
     * Invokes the supplied operation on all peer nodes (on the distributed object manager thread)
     * and blocks the current thread until the execution has completed.
     */
    public static void invokePeerOperation (String name, final PeerManager.Operation op)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(name);
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    MsoyServer.peerMan.applyToNodes(op);
                    waiter.requestCompleted(null);
                } catch (Exception e) {
                    waiter.requestFailed(e);
                }
            }
        });
        waiter.waitForResult();
    }

    /**
     * Resolves the necessary names and converts the supplied list of feed messages to runtime
     * records.
     */
    public static List<FeedMessage> resolveFeedMessages (List<FeedMessageRecord> records)
        throws PersistenceException
    {
        // find out which member and group names we'll need
        IntSet memberIds = new ArrayIntSet(), groupIds = new ArrayIntSet();
        for (FeedMessageRecord record : records) {
            if (record instanceof FriendFeedMessageRecord) {
                memberIds.add(((FriendFeedMessageRecord)record).actorId);
            } else if (record instanceof GroupFeedMessageRecord) {
                groupIds.add(((GroupFeedMessageRecord)record).groupId);
            } else if (record instanceof SelfFeedMessageRecord) {
                memberIds.add(((SelfFeedMessageRecord)record).actorId);
            }
        }

        // generate a lookup for the member names
        IntMap<MemberName> memberNames = IntMaps.newHashIntMap();
        for (MemberName name : MsoyServer.memberRepo.loadMemberNames(memberIds)) {
            memberNames.put(name.getMemberId(), name);
        }

        // generate a lookup for the group names
        IntMap<GroupName> groupNames = IntMaps.newHashIntMap();
        for (GroupRecord group : MsoyServer.groupRepo.loadGroups(groupIds)) {
            groupNames.put(group.groupId, group.toGroupName());
        }

        // create our list of feed messages
        List<FeedMessage> messages = Lists.newArrayList();
        for (FeedMessageRecord record : records) {
            FeedMessage message = record.toMessage();
            if (record instanceof FriendFeedMessageRecord) {
                ((FriendFeedMessage)message).friend =
                    memberNames.get(((FriendFeedMessageRecord)record).actorId);
            } else if (record instanceof GroupFeedMessageRecord) {
                ((GroupFeedMessage)message).group =
                    groupNames.get(((GroupFeedMessageRecord)record).groupId);
            } else if (record instanceof SelfFeedMessageRecord) {
                ((SelfFeedMessage)message).actor =
                    memberNames.get(((SelfFeedMessageRecord)record).actorId);
            }
            messages.add(message);
        }

        return messages;
    }
}
