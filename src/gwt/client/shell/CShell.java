//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Use this to make service calls. */
    public static WebIdent ident;

    /** Our active invitation if we landed at Whirled from an invite, null otherwise (for use if
     * and when we create an account). */
    public static Invitation activeInvite;

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
     * When the client logs onto the Whirled as a guest, they let us know what their id is so that
     * if the guest creates an account we can transfer anything they earned as a guest to their
     * newly created account. This is also called if a player attempts to play a game without
     * having first logged into the server.
     */
    public static void setGuestId (int guestId)
    {
        if (getMemberId() > 0) {
            log("Warning: got guest id but appear to be logged in? " +
                "[memberId=" + getMemberId() + ", guestId=" + guestId + "].");
        } else {
            ident = new WebIdent();
            ident.memberId = guestId;
            // TODO: the code that knows how to do this is in MsoyCredentials which is not
            // accessible to GWT currently for unrelated technical reasons
            ident.token = "G" + guestId;
        }
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

    /** MD5 hashes the supplied text and returns the hex encoded hash value. */
    public native static String md5hex (String text) /*-{
        return $wnd.hex_md5(text);
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
