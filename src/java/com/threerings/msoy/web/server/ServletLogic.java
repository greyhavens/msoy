//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;
import com.threerings.msoy.person.server.persist.SelfFeedMessageRecord;

import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides various services to servlets.
 */
@Singleton
public class ServletLogic
{
    /**
     * Invokes the supplied operation on all peer nodes (on the distributed object manager thread)
     * and blocks the current thread until the execution has completed.
     */
    public void invokePeerOperation (String name, final Function<NodeObject,Void> op)
        throws ServiceException
    {
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(name);
        _omgr.postRunnable(new Runnable() {
            public void run () {
                try {
                    _peerMan.applyToNodes(op);
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
    public List<FeedMessage> resolveFeedMessages (List<FeedMessageRecord> records)
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
        IntMap<MemberName> memberNames = _memberRepo.loadMemberNames(memberIds);

        // generate a lookup for the group names
        IntMap<GroupName> groupNames = IntMaps.newHashIntMap();
        for (GroupRecord group : _groupRepo.loadGroups(groupIds)) {
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

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
}
