//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;

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
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.presents.data.InvocationCodes;

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
            public GroupDetail invokePersistResult () throws Exception {
                // load the group record
                GroupRecord gRec = MsoyServer.groupRepo.loadGroup(groupId);
                if (gRec == null) {
                    return null;
                }
                // load the creator's member record
                MemberRecord mRec = MsoyServer.memberRepo.loadMember(gRec.creatorId);
                if (mRec == null) {
                    log.warning("Couldn't load group creator [groupId=" + groupId +
                        ", creatorId=" + gRec.creatorId + "]");
                    throw new ServiceException(InvocationCodes.INTERNAL_ERROR);
                }
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
    public List<Group> searchForTag (WebCreds creds, final String tag)
        throws ServiceException
    {
        final ServletWaiter<List<Group>> waiter = new ServletWaiter<List<Group>>("searchForTag[" +
            tag + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(waiter) {
            public List<Group> invokePersistResult () throws PersistenceException {
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : MsoyServer.groupRepo.searchForTag(tag)) {
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

    // from interface GroupService
    public TagHistory tagGroup (WebCreds creds, final int groupId, final String tag, 
        final boolean set) throws ServiceException
    {
        final String tagName = tag.trim().toLowerCase();
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            throw new ServiceException("Invalid tag [tag=" + tagName + "]");
        }
        final int memberId = creds.getMemberId();

        final ServletWaiter<TagHistory> waiter = new ServletWaiter<TagHistory>(
            "tagGroup[" + groupId + ", " + tag + ", " + set + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<TagHistory>(waiter) {
            public TagHistory invokePersistResult() throws PersistenceException {
                long now = System.currentTimeMillis();
                
                TagRepository tagRepo = MsoyServer.groupRepo.getTagRepository();
                TagNameRecord tag = tagRepo.getTag(tagName);

                TagHistoryRecord historyRecord = set ?
                    tagRepo.tag(groupId, tag.tagId, memberId, now) :
                    tagRepo.untag(groupId, tag.tagId, memberId, now);
                if (historyRecord != null) {
                    MemberRecord mrec = MsoyServer.memberRepo.loadMember(memberId);
                    TagHistory history = new TagHistory();
                    history.member = mrec.getName();
                    history.tag = tag.tag;
                    history.action = historyRecord.action;
                    history.time = new Date(historyRecord.time.getTime());
                    return history;
                }
                return null;
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public Collection<TagHistory> getRecentTags (WebCreds creds) throws ServiceException
    {
        final int memberId = creds.getMemberId();
        final TagRepository tagRepo = MsoyServer.groupRepo.getTagRepository();
        final ServletWaiter<Collection<TagHistory>> waiter = 
            new ServletWaiter<Collection<TagHistory>>("getRecentTags[]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Collection<TagHistory>>(waiter) {
            public Collection<TagHistory> invokePersistResult () throws PersistenceException {
                MemberRecord memRec = MsoyServer.memberRepo.loadMember(memberId);
                MemberName memName = memRec.getName();
                ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                for (TagHistoryRecord record : tagRepo.getTagHistoryByMember(memberId)) {
                    TagNameRecord tag = record.tagId == -1 ? null :
                       tagRepo.getTag(record.tagId);
                    TagHistory history = new TagHistory();
                    history.member = memName;
                    history.tag = tag == null ? null : tag.tag;
                    history.action = record.action;
                    history.time = new Date(record.time.getTime());
                    list.add(history); 
                }
                return list;
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public Collection<String> getTags (WebCreds creds, final int groupId) throws ServiceException
    {
        final ServletWaiter<Collection<String>> waiter = new ServletWaiter<Collection<String>>(
            "getTags[groupId=" + groupId + "]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Collection<String>>(waiter) {
            public Collection<String> invokePersistResult () throws PersistenceException {
                ArrayList<String> result = new ArrayList<String>();
                for (TagNameRecord tagName : MsoyServer.groupRepo.getTagRepository().
                        getTags(groupId)) {
                    result.add(tagName.tag);
                }
                return result;
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public List<String> getPopularTags (WebCreds creds, final int rows) throws ServiceException
    {
        final ServletWaiter<List<String>> waiter = new ServletWaiter<List<String>>(
            "getPopularTags[]");
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<String>>(waiter) {
            public List<String> invokePersistResult () throws PersistenceException {
                ArrayList<String> result = new ArrayList<String>();
                for (TagPopularityRecord popRec : MsoyServer.groupRepo.getTagRepository().
                        getPopularTags(rows)) {
                    result.add(popRec.tag);
                }
                return result;
            }
        });
        return waiter.waitForResult();
    }

    protected static boolean isValidName (String name) 
    {
        return Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0));
    }
}
