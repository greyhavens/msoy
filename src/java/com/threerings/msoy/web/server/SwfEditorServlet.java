//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.SwfEditorService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class SwfEditorServlet extends MsoyServiceServlet
    implements SwfEditorService
{
    public List<String> getFiles (WebCreds creds)
        throws ServiceException
    {
        // TODO: Implement
        ArrayList<String> files = new ArrayList<String>();

        files.add("Testing");
        return files;
    }
}
