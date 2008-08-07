//
// $Id$

package com.threerings.msoy.swiftly.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    /**
     * The asynchronous version of {@link SwiftlyService#getConnectConfig}.
     */
    void getConnectConfig (int projectId, AsyncCallback<SwiftlyConnectConfig> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getRemixableProjects}.
     */
    void getRemixableProjects (AsyncCallback<List<SwiftlyProject>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getMembersProjects}.
     */
    void getMembersProjects (AsyncCallback<List<SwiftlyProject>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    void createProject (String projectName, byte projectType, boolean remixable,
                               AsyncCallback<SwiftlyProject> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#updateProject}.
     */
    void updateProject (SwiftlyProject project, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadProject}.
     */
    void loadProject (int projectId, AsyncCallback<SwiftlyProject> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectOwner}.
     */
    void getProjectOwner (int projectId, AsyncCallback<MemberName> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#deleteProject}.
     */
    void deleteProject (int projectId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectCollaborators}.
     */
    void getProjectCollaborators (int projectId, AsyncCallback<List<MemberName>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getFriends}.
     */
    void getFriends (AsyncCallback<List<FriendEntry>> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#leaveCollaborators}.
     */
    void leaveCollaborators (int projectId, MemberName name, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SwiftlyService#joinCollaborators}.
     */
    void joinCollaborators (int projectId, MemberName name, AsyncCallback<Void> callback);
}
