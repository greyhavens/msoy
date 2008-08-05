//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import client.util.events.FlashEvents;
import client.util.events.GotGuestIdEvent;
import client.util.events.GotGuestIdListener;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Use this to make service calls. */
    public static WebIdent ident;

    /** Used to communicate with the frame. */
    public static Frame frame;

    /**
     * Returns our member id if we're logged in, 0 if we are not.
     */
    public static int getMemberId ()
    {
        return (ident == null) ? 0 : ident.memberId;
    }

    /**
     * Returns true if we're a guest, false if we're a member.
     */
    public static boolean isGuest ()
    {
        return MemberName.isGuest(getMemberId());
    }

    /**
     * Returns true if we're logged in and have support privileges.
     */
    public static boolean isSupport ()
    {
        return (creds != null) && creds.isSupport;
    }

    /**
     * Returns true if we're logged in and have admin privileges.
     */
    public static boolean isAdmin ()
    {
        return (creds != null) && creds.isAdmin;
    }

    /**
     * Initializes the shell and wires up some listeners.
     */
    public static void init (Frame frame)
    {
        CShell.frame = frame;

        FlashEvents.addListener(new GotGuestIdListener() {
            public void gotGuestId (GotGuestIdEvent event) {
                if (getMemberId() > 0) {
                    log("Warning: got guest id but appear to be logged in? " +
                        "[memberId=" + getMemberId() + ", guestId=" + event.getGuestId() + "].");
                } else {
                    log("Got guest id from Flash " + event.getGuestId() + ".");
                    ident = new WebIdent();
                    ident.memberId = event.getGuestId();
                    // TODO: the code that knows how to do this is in MsoyCredentials which is not
                    // accessible to GWT currently for unrelated technical reasons
                    ident.token = "G" + event.getGuestId();
                }
            }
        });
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (Throwable error)
    {
        if (error instanceof ServiceException) {
            return serverError(error.getMessage());
        } else {
            return _smsgs.getString("internal_error");
        }
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (String error)
    {
        // ConstantsWithLookup can't handle things that don't look like method names, yay!
        if (error.startsWith("m.") || error.startsWith("e.")) {
            error = error.substring(2);
        }
        try {
            return _smsgs.getString(error);
        } catch (MissingResourceException e) {
            // looking up a missing translation message throws an exception, yay!
            return "[" + error + "]";
        }
    }

    /** Reports a log message to the console. */
    public static void log (String message)
    {
        if (GWT.isScript()) {
            consoleLog(message);
        } else {
            GWT.log(message, null);
        }
    }

    /** Reports a log message and exception stack trace to the console. */
    public static void log (String message, Throwable error)
    {
        if (GWT.isScript()) {
            consoleLog(message + ": " + error); // TODO: log stack trace?
        } else {
            GWT.log(message, error);
        }
    }

    /**
     * Returns a partner identifier when we're running in partner cobrand mode, null when we're
     * running in the full Whirled environment.
     */
    public static native String getPartner () /*-{
        return $doc.whirledPartner;
    }-*/;

    /**
     * Records a log message to the JavaScript console.
     */
    protected static native void consoleLog (String message) /*-{
        if ($wnd.console) {
            $wnd.console.log(message);
        }
    }-*/;

    protected static final ServerMessages _smsgs = GWT.create(ServerMessages.class);
}
