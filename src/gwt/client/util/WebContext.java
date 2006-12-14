//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.GameServiceAsync;
import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileServiceAsync;
import com.threerings.msoy.web.client.WebUserServiceAsync;

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

    /** Messages shared by all client interfaces. */
    public GlobalMessages gmsgs;

    /** Contains translations for server-supplied messages. */
    public ServerMessages smsgs;

    /** Reports a log message to the console. */
    public void log (String message)
    {
        if (GWT.isScript()) {
            consoleLog(message);
        } else {
            GWT.log(message, null);
        }
    }

    /** Reports a log message and exception stack trace to the console. */
    public void log (String message, Exception error)
    {
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
        if ($wnd.console) {
            $wnd.console.log(message);
        }
    }-*/;
}
