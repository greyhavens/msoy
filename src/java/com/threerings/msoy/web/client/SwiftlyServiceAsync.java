//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.SwiftlyConfig;
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
     * The asynchronous version of {@link SwiftlyService#getProjectTypes}.
     */
    public void getProjectTypes (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    public void createProject (
        WebCreds creds, String projectName, int projectType, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadProject}.
     */
    public void loadProject (WebCreds creds, int projectId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#loadSwiftlyConfig}.
     */
    public void loadSwiftlyConfig (WebCreds creds, AsyncCallback callback);
}
