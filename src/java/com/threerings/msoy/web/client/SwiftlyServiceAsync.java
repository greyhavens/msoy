//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link SwiftlyService}.
 */
public interface SwiftlyServiceAsync
{
    public List getFiles (WebCreds creds, AsyncCallback callback);
}
