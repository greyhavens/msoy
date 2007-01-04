//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;
import com.google.gwt.user.client.rpc.RemoteService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines Swiftly Editor services available to the java web client.
 */
public interface SwfEditorService extends RemoteService
{
    public List getFiles (WebCreds creds)
        throws ServiceException;
}
