//
// $Id$

package com.threerings.msoy.server;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.GroupName;
import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.GroupRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manage msoy groups.
 */
public class GroupManager
{
    public void init (GroupRepository groupRepo, MemberRepository memberRepo)
    {
        _groupRepo = groupRepo;
        _memberRepo = memberRepo;
    }
   
    /**
     * Fetches all group records that start with the given character, and sends them back in web 
     * object format.
     * This method will most likely become a pager in the near future, or we
     * will be returning some kind of summary object.
     */
    public void getGroups (final String startingCharacter, ResultListener<List<Group>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(listener) {
            public List<Group> invokePersistResult () throws PersistenceException {
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : _groupRepo.findGroups(startingCharacter)) {
                    groups.add(gRec.toGroupObject());
                }
                return groups;
            }
        });
    }

    /**
     * Searches all group records and sends them back in web object format.
     */
    public void searchGroups (final String searchString, ResultListener<List<Group>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Group>>(listener) {
            public List<Group> invokePersistResult () throws PersistenceException {
                List<Group> groups = new ArrayList<Group>();
                for (GroupRecord gRec : _groupRepo.searchGroups(searchString)) {
                    groups.add(gRec.toGroupObject());
                }
                return groups;
            }
        });
    }

    /**
     * Creates a new group record in the database and return a {@link Group} for
     * it. This method assigns the group a new, unique id.
     * 
     * TODO: Sanity checks on group name.
     */
    public void createGroup (final Group groupDef, final GroupExtras extrasDef,    
        ResultListener<Group> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Group>(listener) {
            public Group invokePersistResult () throws PersistenceException {
                GroupRecord gRec = new GroupRecord();
                gRec.name = groupDef.name;
                gRec.blurb = groupDef.blurb;
                gRec.creatorId = groupDef.creatorId;
                gRec.creationDate = new Timestamp(groupDef.creationDate.getTime());
                gRec.policy = groupDef.policy;
                if (groupDef.logo != null) {
                    gRec.logoMimeType = groupDef.logo.mimeType;
                    gRec.logoMediaHash = groupDef.logo.hash;
                    gRec.logoMediaConstraint = groupDef.logo.constraint;
                }
                gRec.homepageUrl = extrasDef.homepageUrl;
                gRec.charter = extrasDef.charter;
                if (extrasDef.infoBackground != null) {
                    gRec.infoBackgroundMimeType = extrasDef.infoBackground.mimeType;
                    gRec.infoBackgroundHash = extrasDef.infoBackground.hash;
                }
                if (extrasDef.detailBackground != null) { 
                    gRec.detailBackgroundMimeType = extrasDef.detailBackground.mimeType;
                    gRec.detailBackgroundHash = extrasDef.detailBackground.hash;
                }
                if (extrasDef.peopleBackground != null) {
                    gRec.peopleBackgroundMimeType = extrasDef.peopleBackground.mimeType;
                    gRec.peopleBackgroundHash = extrasDef.peopleBackground.hash;
                }

                // create the group and then add the creator to it
                _groupRepo.createGroup(gRec);
                _groupId = gRec.groupId;
                _groupRepo.joinGroup(_groupId, gRec.creatorId, GroupMembership.RANK_MANAGER);
    
                return gRec.toGroupObject();
            }

            public void handleSuccess () {
                super.handleSuccess();

                updateMemberGroup(groupDef.creatorId, _groupId, groupDef.name, 
                    GroupMembership.RANK_MANAGER);
            }

            protected int _groupId;
        });
    }

    /**
     * Updates a group record in the database with new data. Only non-null/non-zero parameters
     * are used for the update, and data is not read back from the database. This is a low-level
     * method without privilige checks; it's up to the callers to secure it.
     */
    public void updateGroup (final Group groupDef, final GroupExtras extrasDef, 
        ResultListener<Void> listener) 
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult () throws PersistenceException {
                GroupRecord gRec = MsoyServer.groupRepo.loadGroup(groupDef.groupId);
                if (gRec == null) {
                    throw new PersistenceException("Group not found! [id=" + groupDef.groupId + 
                        "]");
                }
                Map<String, Object> updates = gRec.findUpdates(groupDef, extrasDef);
                if (updates.size() > 0) {
                    MsoyServer.groupRepo.updateGroup(groupDef.groupId, updates);
                }
                return null;
            }
        });
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public void getGroupDetail (final int groupId, ResultListener<GroupDetail> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<GroupDetail>(listener) {
            public GroupDetail invokePersistResult () throws PersistenceException {
                // load the group record
                GroupRecord gRec = _groupRepo.loadGroup(groupId);
                // load the creator's member record
                MemberRecord mRec = _memberRepo.loadMember(gRec.creatorId);
                // set up the detail
                GroupDetail detail = new GroupDetail();
                detail.creator = mRec.getName();
                detail.group = gRec.toGroupObject();
                detail.extras = gRec.toExtrasObject();
                ArrayList<GroupMembership> members = new ArrayList<GroupMembership>();
                detail.members = members;
                for (GroupMembershipRecord gmRec : _groupRepo.getMembers(groupId)) {
                    mRec = _memberRepo.loadMember(gmRec.memberId);
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
    }

    /**
     * Fetches the groups a given person is a member of, as {@link GroupMembership} records.
     * This method does not distinguish between a nonexistent person, and a person who is
     * a member of no groups; both situations yield empty collections.
     */
    public void getMembershipGroups (final int memberId, final boolean canInvite,
                                     ResultListener<List<GroupMembership>> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<GroupMembership>>(listener) {
            public List<GroupMembership> invokePersistResult () throws PersistenceException {
                MemberName mName = _memberRepo.loadMember(memberId).getName();
                List<GroupMembership> result = new ArrayList<GroupMembership>();
                for (GroupMembershipRecord gmRec : _groupRepo.getMemberships(memberId)) {
                    GroupRecord gRec = _groupRepo.loadGroup(gmRec.groupId);
                    // if we're only including groups we can invite to, strip out exclusive groups
                    // of which we're not managers
                    if (canInvite && gRec.policy == Group.POLICY_EXCLUSIVE &&
                        gmRec.rank != GroupMembership.RANK_MANAGER) {
                        continue;
                    }
                    result.add(gmRec.toGroupMembership(gRec, mName));
                }
                return result;
            }
        });
    }
    
    /**
     * Cancels the given person's membership in the given group.
     */
    public void leaveGroup (final int groupId, final int memberId, ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.leaveGroup(groupId, memberId);
                // remove groups with 0 memebers left
                if (_groupRepo.countMembers(groupId) == 0) {
                    GroupRecord group = new GroupRecord();
                    group.groupId = groupId;
                    _groupRepo.deleteGroup(group);
                }
                return null;
            }

            public void handleSuccess () {
                super.handleSuccess();

                // data made it to the db, update their member object if they're online
                MemberObject member = MsoyServer.lookupMember(memberId);
                if (member != null) {
                    member.removeFromGroups(GroupName.makeKey(groupId));
                }
            }
        });
    }

    /**
     * Makes the given person a member of the given group, of the given rank.
     */
    public void joinGroup (final int groupId, final int memberId, final byte rank,
                           ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.joinGroup(groupId, memberId, rank);
                _groupName = MsoyServer.groupRepo.loadGroup(groupId).name;
                return null;
            }

            public void handleSuccess () {
                super.handleSuccess();

                updateMemberGroup(memberId, groupId, _groupName, rank);
            }

            protected String _groupName;
        });        
    }

    /**
     * Returns the rank of a group member, or null.
     */
    public void getRank (final int groupId, final int memberId, ResultListener<Byte> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Byte>(listener) {
            public Byte invokePersistResult() throws PersistenceException {
                GroupMembershipRecord gmr = _groupRepo.getMembership(groupId, memberId);
                return gmr != null ? gmr.rank : null;
            }
        });
    }

    /**
     * Sets the rank of a group member. Throws an exception if there is no
     * such person, no such group, or the person is not a member of the group.
     */
    public void setRank (final int groupId, final int memberId, final byte newRank,
                         ResultListener<Void> listener)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>(listener) {
            public Void invokePersistResult() throws PersistenceException {
                _groupRepo.setRank(groupId, memberId, newRank);
                return null;
            }
        });
    }

    /**
     * Updates or Adds to the groups set on the given member's object, as appropriate.
     */
    protected void updateMemberGroup (int memberId, int groupId, String groupName, 
        byte groupRank)
    {
        MemberObject mobj = MsoyServer.lookupMember(memberId);
        if (mobj == null) {
            return; // no need to update anything
        }

        // see if we're just updating their rank
        GroupMembership gm = mobj.groups.get(GroupName.makeKey(groupId));
        if (gm != null) {
            gm.rank = groupRank;
            mobj.updateGroups(gm);
            return;
        }

        // otherwise they are newly joined
        gm = new GroupMembership();
        // gm.member specifically left null
        gm.group = new GroupName(groupName, groupId);
        gm.rank = groupRank;
        mobj.addToGroups(gm);
    } 

    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;

    /** Provides access to the persistent member data. */
    protected MemberRepository _memberRepo;
}
