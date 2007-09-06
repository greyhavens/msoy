//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    /**
     * The asynchronous version of {@link SwiftlyService#getConnectConfig}.
     */
    public void getConnectConfig (WebIdent ident, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getRemixableProjects}.
     */
    public void getRemixableProjects (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getMembersProjects}.
     */
    public void getMembersProjects (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    public void createProject (WebIdent ident, String projectName, byte projectType,
                               boolean remixable, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#updateProject}.
     */
    public void updateProject (WebIdent ident, SwiftlyProject project, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadProject}.
     */
    public void loadProject (WebIdent ident, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectOwner}.
     */
    public void getProjectOwner (WebIdent ident, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#deleteProject}.
     */
    public void deleteProject (WebIdent ident, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getProjectCollaborators}.
     */
    public void getProjectCollaborators (WebIdent ident, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#getFriends}.
     */
    public void getFriends (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#leaveCollaborators}.
     */
    public void leaveCollaborators (WebIdent ident, int projectId, MemberName name,
                                    AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#joinCollaborators}.
     */
    public void joinCollaborators (WebIdent ident, int projectId, MemberName name,
                                   AsyncCallback callback);
}
