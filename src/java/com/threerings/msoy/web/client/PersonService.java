//
// $Id$

package com.threerings.msoy.web.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Defines "person page" services available to the GWT/AJAX web client.
 */
public interface PersonService extends RemoteService
{
    /**
     * Loads the blurbs for the specified member's personal page. The first
     * entry in the list will be information on the page layout and subsequent
     * entries will be data for each of the blurbs on the page.
     */
    public ArrayList loadBlurbs (int memberId)
        throws ServiceException;
}
