//
// $Id$

package com.threerings.msoy.notifications.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Service for accessing messages to the user.
 */
@RemoteServiceRelativePath(value=NotificationsService.REL_PATH)
public interface NotificationsService extends RemoteService
{
    public static final String ENTRY_POINT = "/notifications";
    public static final String REL_PATH = "../../.." + NotificationsService.ENTRY_POINT;

    /**
     * Gets the current list of notifications for the logged-in user.
     */
    public List<Notification> getNotifications ()
        throws ServiceException;
}
