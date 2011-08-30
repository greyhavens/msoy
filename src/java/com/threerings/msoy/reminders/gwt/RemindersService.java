//
// $Id$

package com.threerings.msoy.reminders.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

/**
 * Service for accessing messages reminding the user to do things.
 */
@RemoteServiceRelativePath(value=RemindersService.REL_PATH)
public interface RemindersService extends RemoteService
{
    public static final String ENTRY_POINT = "/notifications";
    public static final String REL_PATH = "../../.." + RemindersService.ENTRY_POINT;

    /**
     * Gets the current list of reminders for the logged-in user and given application.
     */
    public List<Reminder> getReminders (int appId)
        throws ServiceException;
}
