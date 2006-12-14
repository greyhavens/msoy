//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;

import java.util.logging.Level;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.world.data.MsoySceneModel;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class GroupServlet extends RemoteServiceServlet
    implements GroupService
{
    // from interface GroupService
    public GroupDetail getGroupDetail (WebCreds creds, final int groupId)
        throws ServiceException
    {
        // TODO: validate creds
        final ServletWaiter<GroupDetail> waiter =
            new ServletWaiter<GroupDetail>("getGroupDetail[" + groupId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.getGroupDetail(groupId, waiter);
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
    // java.lang.Character is not Comparable in GWT.
    public List<String> getCharacters (WebCreds creds)
        throws ServiceException
    {
        try {
            return MsoyServer.groupRepo.getCharacters();
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to fetch the list of group name prefixes.", pe);
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<Group> getGroups (WebCreds creds)
        throws ServiceException
    {
        final ServletWaiter<List<Group>> waiter = new ServletWaiter<List<Group>>("getGroups[]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.getGroups(waiter);
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
                MsoyServer.memberMan.getMembershipGroups(memberId, canInvite, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public Group createGroup (WebCreds creds, final Group group) throws ServiceException
    {
        // TODO: validate creds

        if(!isValidName(group.name)) {
            throw new ServiceException("group", "m.invalid_group_name");
        }

        final ServletWaiter<Group> waiter = new ServletWaiter<Group>("createGroup[" + group + "]");
        group.creationDate = new Date(System.currentTimeMillis());
        group.creatorId = creds.memberId;
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.createGroup(group, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public void updateGroup (WebCreds creds, final Group group) throws ServiceException
    {
        // TODO: validate creds
        
        if(!isValidName(group.name)) {
            throw new ServiceException("group", "m.invalid_group_name");
        }

        final ServletWaiter<Void> waiter = new ServletWaiter<Void>("updateGroup[" + group + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.updateGroup(
                    group.groupId, group.name, group.homepageUrl, group.blurb, group.charter, 
                    group.logo, group.policy, waiter);
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
            "createGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.leaveGroup(groupId, memberId, waiter);
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
            "createGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.joinGroup(
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
            "createGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.setRank(groupId, memberId, newRank, waiter);
            }
        });
        waiter.waitForResult();
    }

    protected static boolean isValidName (String name) 
    {
        return Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0));
    }
}
