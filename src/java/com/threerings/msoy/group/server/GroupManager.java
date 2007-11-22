//
// $Id$

package com.threerings.msoy.group.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

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
     * Cancels the given person's membership in the given group.
     */
    public void leaveGroup (final int groupId, final int memberId, ResultListener<Void> listener)
    {
        assert(groupId != 0 && memberId != 0);
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
                // PEER TODO: user may be resolved on another world server
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
        assert(groupId != 0 && memberId != 0 && GroupMembership.isValidRank(rank));
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
        assert(groupId != 0 && memberId != 0);
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Byte>(listener) {
            public Byte invokePersistResult() throws PersistenceException {
                GroupMembershipRecord gmr = _groupRepo.getMembership(groupId, memberId);
                return gmr != null ? gmr.rank : null;
            }
        });
    }

    /**
     * Updates or Adds to the groups set on the given member's object, as appropriate.
     */
    protected void updateMemberGroup (int memberId, int groupId, String groupName, byte groupRank)
    {
        // PEER TODO: user may be resolved on another world server
        assert(groupId != 0 && memberId != 0 && groupName != null &&
               GroupMembership.isValidRank(groupRank));
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
