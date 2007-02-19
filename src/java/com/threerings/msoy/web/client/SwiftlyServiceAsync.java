//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    /**
     * The asynchronous version of {@link SwiftlyService#getProjects}.
     */
    public void getProjects (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link SwiftlyService#createProject}.
     */
    public void createProject (WebCreds creds, SwiftlyProject project,
                                         AsyncCallback callback);
}
