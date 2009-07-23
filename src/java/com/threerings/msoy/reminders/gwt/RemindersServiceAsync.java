//
// $Id$

package com.threerings.msoy.reminders.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous version of {@link RemindersService}.
 */
public interface RemindersServiceAsync
{
    /**
     * The asynchronous version of {@link RemindersService#getNotifications}.
     */
    public void getReminders (AsyncCallback<List<Reminder>> callback);
}
