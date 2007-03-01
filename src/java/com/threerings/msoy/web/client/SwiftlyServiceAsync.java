//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    /**
     * The asynchronous version of {@link SwiftlyService#getRemixableProjects}.
     */
    public void getRemixableProjects (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getMembersProjects}.
     */
    public void getMembersProjects (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    public void createProject (WebCreds creds, String projectName, byte projectType,
                               boolean remixable, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#updateProject}.
     */
    public void updateProject (WebCreds creds, SwiftlyProject project, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadProject}.
     */
    public void loadProject (WebCreds creds, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadConnectConfig}.
     */
    public void loadConnectConfig (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectCollaborators}.
     */
    public void getProjectCollaborators (WebCreds creds, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getFriends}.
     */
    public void getFriends (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#leaveCollaborators}.
     */
    public void leaveCollaborators (WebCreds creds, int projectId, int memberId,
                                         AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#joinCollaborators}.
     */
    public void joinCollaborators (WebCreds creds, int projectId, int memberId,
                                    AsyncCallback callback);
}
