//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#getGroupsList}
     */
    public void getGroupsList (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#searchGroups}
     */
    public void searchGroups (WebCreds creds, String searchString, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupHomeId}
     */
    public void getGroupHomeId (WebCreds creds, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#getMembershipGroups}
     */
    public void getMembershipGroups (WebCreds creds, int memberId, boolean canInvite,
                                     AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.loadGroup} 
     */
    public void getGroupDetail (WebCreds creds, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.updateGroup} 
     */
    public void updateGroup (WebCreds creds, Group group, GroupExtras extras, 
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.createGroup} 
     */
    public void createGroup (WebCreds creds, Group group, GroupExtras extras, 
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.leaveGroup} 
     */
    public void leaveGroup (WebCreds creds, int groupId, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.joinGroup} 
     */
    public void joinGroup (WebCreds creds, int groupId, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.updateMemberRank}
     */
    public void updateMemberRank (WebCreds creds, int groupId, int memberId, byte newRank,
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.tagGroup}
     */
    public void tagGroup (WebCreds creds, int groupId, String tag, boolean set,
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getRecentTags}
     */
    public void getRecentTags (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getTags}
     */
    public void getTags (WebCreds creds, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getPopularTags}
     */
    public void getPopularTags (WebCreds creds, int rows, AsyncCallback callback);
}
