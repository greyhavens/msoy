//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.data.WebCreds;

/**
 * Contains a reference to the various bits that we're likely to need in the
 * web client interface.
 */
public class WebContext
{
    /** Our credentials or null if we are not logged in. */
    public WebCreds creds;

    /** Provides user-related services. */
    public WebUserServiceAsync usersvc;

    /** Provides item-related services. */
    public ItemServiceAsync itemsvc;

    /** Provides profile-related services. */
    public ProfileServiceAsync profilesvc;

    /** Provides person-related services. */
    public PersonServiceAsync personsvc;

    /** Provides member-related service. */
    public MemberServiceAsync membersvc;

    /** Provides group-related services. */
    public GroupServiceAsync groupsvc;

    /** Provides mail-related services. */
    public MailServiceAsync mailsvc;

    /** Provides catalog-related services. */
    public CatalogServiceAsync catalogsvc;

    /** Provides game-related services. */
    public GameServiceAsync gamesvc;

    /** Reports a log message to the console. */
    public void log (String message)
    {
        // TODO: disable this in production
        if (GWT.isScript()) {
            consoleLog(message);
        } else {
            GWT.log(message, null);
        }
    }

    /** Reports a log message and exception stack trace to the console. */
    public void log (String message, Exception error)
    {
        // TODO: disable this in production
        if (GWT.isScript()) {
            consoleLog(message + ": " + error); // TODO: log stack trace?
        } else {
            GWT.log(message, error);
        }
    }

    /**
     * Records a log message to the JavaScript console.
     */
    protected static native void consoleLog (String message) /*-{
        $wnd.console.log(message);
    }-*/;
}
