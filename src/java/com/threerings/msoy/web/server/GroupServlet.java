//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.GroupMembershipRecord;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class GroupServlet extends MsoyServiceServlet
    implements GroupService
{
    // from GroupService
    public List<Group> getGroupsList (WebCreds creds)
        throws ServiceException
    {
        final ServletWaiter<List<Group>> waiter = new ServletWaiter<List<Group>>("getGroupsList[]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(waiter) {
            public List<Group> invokePersistResult () throws PersistenceException {
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : MsoyServer.groupRepo.getGroupsList()) {
                    groups.add(gRec.toGroupObject());
                }
                return groups;
            }
        });
        return waiter.waitForResult();
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public GroupDetail getGroupDetail (WebCreds creds, final int groupId)
        throws ServiceException
    {
        // TODO: validate creds
        final ServletWaiter<GroupDetail> waiter =
            new ServletWaiter<GroupDetail>("getGroupDetail[" + groupId + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<GroupDetail>(waiter) {
            public GroupDetail invokePersistResult () throws PersistenceException {
                // load the group record
                GroupRecord gRec = MsoyServer.groupRepo.loadGroup(groupId);
                // load the creator's member record
                MemberRecord mRec = MsoyServer.memberRepo.loadMember(gRec.creatorId);
                // set up the detail
                GroupDetail detail = new GroupDetail();
                detail.creator = mRec.getName();
                detail.group = gRec.toGroupObject();
                detail.extras = gRec.toExtrasObject();
                ArrayList<GroupMembership> members = new ArrayList<GroupMembership>();
                detail.members = members;
                for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMembers(groupId)) {
                    mRec = MsoyServer.memberRepo.loadMember(gmRec.memberId);
                    GroupMembership membership = new GroupMembership();
                    // membership.group left null intentionally 
                    membership.member = mRec.getName();
                    membership.rank = gmRec.rank;
                    membership.rankAssignedDate = gmRec.rankAssigned.getTime();
                    members.add(membership);
                }
                return detail;
            }
        });
        return waiter.waitForResult();
    }

    // from GroupService
    public Integer getGroupHomeId (WebCreds creds, final int groupId)
        throws ServiceException
    {
        final ServletWaiter<Integer> waiter =new ServletWaiter<Integer>(
            "getHomeId[" + groupId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_GROUP, groupId, waiter);
            }
        });
        return waiter.waitForResult();
    }
    
    // from interface GroupService
    public List<Group> searchGroups (WebCreds creds, final String searchString) 
        throws ServiceException
    {
        final ServletWaiter<List<Group>> waiter = new ServletWaiter<List<Group>>("searchGroups[" +
            searchString + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(waiter) {
            public List<Group> invokePersistResult () throws PersistenceException { 
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : MsoyServer.groupRepo.searchGroups(searchString)) {
                    groups.add(gRec.toGroupObject());
                }
                return groups;
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public List<GroupMembership> getMembershipGroups (WebCreds creds, final int memberId,
                                                      final boolean canInvite)
        throws ServiceException
    {
        final ServletWaiter<List<GroupMembership>> waiter =
            new ServletWaiter<List<GroupMembership>>("getMembershipGroups[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.getMembershipGroups(memberId, canInvite, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public Group createGroup (WebCreds creds, final Group group, final GroupExtras extras) 
        throws ServiceException
    {
        // TODO: validate creds

        if(!isValidName(group.name)) {
            throw new ServiceException("m.invalid_group_name");
        }

        final ServletWaiter<Group> waiter = new ServletWaiter<Group>("createGroup[" + group + "]");
        group.creationDate = new Date(System.currentTimeMillis());
        group.creatorId = creds.getMemberId();
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.createGroup(group, extras, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public void updateGroup (WebCreds creds, final Group group, final GroupExtras extras) 
        throws ServiceException
    {
        // TODO: validate creds
        
        if(!isValidName(group.name)) {
            throw new ServiceException("m.invalid_group_name");
        }

        final ServletWaiter<Void> waiter = new ServletWaiter<Void>("updateGroup[" + group + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult () throws PersistenceException {
                GroupRecord gRec = MsoyServer.groupRepo.loadGroup(group.groupId);
                if (gRec == null) {
                    throw new PersistenceException("Group not found! [id=" + group.groupId + 
                        "]");
                }
                Map<String, Object> updates = gRec.findUpdates(group, extras);
                if (updates.size() > 0) {
                    MsoyServer.groupRepo.updateGroup(group.groupId, updates);
                }
                return null;
            }
        });        
        waiter.waitForResult();
    }

    // from interface GroupService
    public void leaveGroup (WebCreds creds, final int groupId, final int memberId)
        throws ServiceException
    {
        // TODO: validate creds
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "leaveGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.leaveGroup(groupId, memberId, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface GroupService
    public void joinGroup (WebCreds creds, final int groupId, final int memberId)
        throws ServiceException
    {
        // TODO: validate creds
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "joinGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.joinGroup(
                    groupId, memberId, GroupMembership.RANK_MEMBER, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface GroupService
    public void updateMemberRank (WebCreds creds, final int groupId, final int memberId,
                                  final byte newRank) 
        throws ServiceException
    {
        // TODO: validate creds
        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "updateMemberRank[" + groupId + ", " + memberId + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(waiter) {
            public Void invokePersistResult() throws PersistenceException {
                MsoyServer.groupRepo.setRank(groupId, memberId, newRank);
                return null;
            }
        });
        waiter.waitForResult();
    }

    protected static boolean isValidName (String name) 
    {
        return Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0));
    }
}
