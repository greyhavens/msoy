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
     * Updates the specified member's distributed object with their new group status.
     */
    public void updateMemberGroup (int memberId, int groupId, String groupName, byte groupRank)
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

    /** Provides access to persistent group data. */
    protected GroupRepository _groupRepo;

    /** Provides access to the persistent member data. */
    protected MemberRepository _memberRepo;
}
