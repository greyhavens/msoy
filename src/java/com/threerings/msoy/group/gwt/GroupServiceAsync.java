//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.web.gwt.TagHistory;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#getGalaxyData}
     */
    void getGalaxyData (AsyncCallback<GalaxyData> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroups}
     */
    void getGroups (int offset, int count, boolean needCount,
                    AsyncCallback<GroupService.GroupsResult> callback);

    /**
     * The asynchronous version of {@link GroupService#searchGroups}
     */
    void searchGroups (String searchString, AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#searchForTag}
     */
    void searchForTag (String tag, AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupInfo}
     */
    void getGroupInfo (int groupId, AsyncCallback<GroupService.GroupInfo> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupHomeId}
     */
    void getGroupHomeId (int groupId, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link GroupService#getMembershipGroups}
     */
    void getMembershipGroups (int memberId, boolean canInvite,
                              AsyncCallback<List<GroupMembership>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupDetail}
     */
    void getGroupDetail (int groupId, AsyncCallback<GroupDetail> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupMembers}
     */
    void getGroupMembers (int groupId, AsyncCallback<GroupService.MembersResult> callback);

    /**
     * The asynchronous version of {@link GroupService#transferRoom}
     */
    void transferRoom (int groupId, int sceneId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#updateGroup}
     */
    void updateGroup (Group group, GroupExtras extras, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#createGroup}
     */
    void createGroup (Group group, GroupExtras extras, AsyncCallback<Group> callback);

    /**
     * The asynchronous version of {@link GroupService#leaveGroup}
     */
    void leaveGroup (int groupId, int memberId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#joinGroup}
     */
    void joinGroup (int groupId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#updateMemberRank}
     */
    void updateMemberRank (int groupId, int memberId, byte newRank, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#tagGroup}
     */
    void tagGroup (int groupId, String tag, boolean set, AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link GroupService#getRecentTags}
     */
    void getRecentTags (AsyncCallback<List<TagHistory>> callback);

    /**
     * The asynchronous version of {@link GroupService#getTags}
     */
    void getTags (int groupId, AsyncCallback<List<String>> callback);

    /**
     * The asynchronous version of {@link GroupService#getMyGroups}
     */
    void getMyGroups (byte sortMethod, AsyncCallback<List<MyGroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGameGroups}
     */
    void getGameGroups (int gameId, AsyncCallback<List<GroupMembership>> callback);
}
