//
// $Id$

package com.threerings.msoy.notifications.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous version of {@link NotificationsService}.
 */
public interface NotificationsServiceAsync
{
    /**
     * The asynchronous version of {@link NotificationService#getNotifications}.
     */
    public void getNotifications (AsyncCallback<List<Notification>> callback);
}
