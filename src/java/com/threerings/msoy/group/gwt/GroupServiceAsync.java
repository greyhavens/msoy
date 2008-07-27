//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#getGalaxyData}
     */
    void getGalaxyData (WebIdent ident, AsyncCallback<GalaxyData> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupsList}
     */
    void getGroupsList (WebIdent ident, AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#searchGroups}
     */
    public void searchGroups (WebIdent ident, String searchString,
                              AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#searchForTag}
     */
    void searchForTag (WebIdent ident, String tag, AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupInfo}
     */
    public void getGroupInfo (WebIdent ident, int groupId,
                              AsyncCallback<GroupService.GroupInfo> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupHomeId}
     */
    void getGroupHomeId (WebIdent ident, int groupId, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link GroupService#getMembershipGroups}
     */
    public void getMembershipGroups (WebIdent ident, int memberId, boolean canInvite,
                                     AsyncCallback<List<GroupMembership>> callback);

    /**
     * The asynchronous version of {@link GroupService.getGroupDetail}
     */
    void getGroupDetail (WebIdent ident, int groupId, AsyncCallback<GroupDetail> callback);

    /**
     * The asynchronous version of {@link GroupService.getGroupMembers}
     */
    public void getGroupMembers (WebIdent ident, int groupId,
                                 AsyncCallback<GroupService.MembersResult> callback);

    /**
     * The asynchronous version of {@link GroupService.getGroupRooms}
     */
    public void getGroupRooms (WebIdent ident, int groupId,
                               AsyncCallback<GroupService.RoomsResult> callback);

    /**
     * The asynchronous version of {@link GroupService.transferRoom}
     */
    public void transferRoom (WebIdent ident, int groupId, int sceneId,
                              AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService.updateGroup}
     */
    public void updateGroup (WebIdent ident, Group group, GroupExtras extras,
                             AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService.createGroup}
     */
    public void createGroup (WebIdent ident, Group group, GroupExtras extras,
                             AsyncCallback<Group> callback);

    /**
     * The asynchronous version of {@link GroupService.leaveGroup}
     */
    public void leaveGroup (WebIdent ident, int groupId, int memberId,
                            AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService.joinGroup}
     */
    void joinGroup (WebIdent ident, int groupId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService.updateMemberRank}
     */
    public void updateMemberRank (WebIdent ident, int groupId, int memberId, byte newRank,
                                  AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService.tagGroup}
     */
    public void tagGroup (WebIdent ident, int groupId, String tag, boolean set,
                          AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link GroupService.getRecentTags}
     */
    void getRecentTags (WebIdent ident, AsyncCallback<Collection<TagHistory>> callback);

    /**
     * The asynchronous version of {@link GroupService.getTags}
     */
    void getTags (WebIdent ident, int groupId, AsyncCallback<Collection<String>> callback);

    /**
     * The asynchronous version of {@link GroupService#getMyGroups}
     */
    public void getMyGroups (WebIdent ident, byte sortMethod,
                             AsyncCallback<List<MyGroupCard>> callback);
}
