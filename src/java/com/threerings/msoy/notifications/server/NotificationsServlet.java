//
// $Id$

package com.threerings.msoy.notifications.server;

import java.util.List;

import com.threerings.msoy.notifications.gwt.Notification;
import com.threerings.msoy.notifications.gwt.NotificationsService;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation for {@link NotificationService}.
 */
public class NotificationsServlet extends MsoyServiceServlet
    implements NotificationsService
{
    @Override // from NotificationService
    public List<Notification> getNotifications ()
    {
        // TODO
        return null;
    }
}
