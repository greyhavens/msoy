//
// $Id$

package com.threerings.msoy.reminders.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Provides the asynchronous version of {@link RemindersService}.
 */
public interface RemindersServiceAsync
{
    /**
     * The async version of {@link RemindersService#getReminders}.
     */
    void getReminders (int appId, AsyncCallback<List<Reminder>> callback);
}
