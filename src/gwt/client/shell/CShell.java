//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

import com.threerings.gwt.util.Console;

import com.threerings.msoy.web.gwt.ClientMode;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Used to communicate with the frame. */
    public static Frame frame;

    /**
     * Returns our authentication token, or null if we don't have one.
     */
    public static String getAuthToken ()
    {
        return (creds == null) ? null : creds.token;
    }

    /**
     * Returns our member id if we're logged in, 0 if we are not.
     */
    public static int getMemberId ()
    {
        return (creds == null) ? 0 : creds.name.getMemberId();
    }

    /**
     * Returns true <em>iff</em> we're an ephemeral guest (not a permaguest or member).
     */
    public static boolean isGuest ()
    {
        return getMemberId() == 0;
    }

    /**
     * Returns true <em>iff</em> we're a permaguest (not an ephemeral guest or member).
     */
    public static boolean isPermaguest ()
    {
        return (creds != null) && (creds.role == WebCreds.Role.PERMAGUEST);
    }

    /**
     * Returns true if we have some sort of account. We may be a permaguest or a registered member,
     * or anything "greater" (validated, support, admin, etc.).
     */
    public static boolean isMember ()
    {
        return (creds != null) && creds.isMember();
    }

    /**
     * Returns true if we are a registered user or "greater", false if we're a guest or permaguest.
     */
    public static boolean isRegistered ()
    {
        return (creds != null) && creds.isRegistered();
    }

    /**
     * Returns true if we're logged in and are a subscriber or "greater", false otherwise.
     */
    public static boolean isSubscriber ()
    {
        return (creds != null) && creds.isSubscriber();
    }

    /**
     * Returns true if we're logged in and have support+ privileges.
     */
    public static boolean isSupport ()
    {
        return (creds != null) && creds.isSupport();
    }

    /**
     * Returns true if we're logged in and have admin+ privileges.
     */
    public static boolean isAdmin ()
    {
        return (creds != null) && creds.isAdmin();
    }

    /**
     * Returns true if we're logged in and have maintainer privileges.
     */
    public static boolean isMaintainer ()
    {
        return (creds != null) && creds.isMaintainer();
    }

    /**
     * Returns true if we are a registered user with a validated email address.
     */
    public static boolean isValidated ()
    {
        return (creds != null) && creds.validated;
    }

    /**
     * Returns true if we are a registered user that is still a "newbie".
     */
    public static boolean isNewbie ()
    {
        return (creds != null) && creds.isNewbie;
    }

    /**
     * Initializes the shell and wires up some listeners.
     */
    public static void init (Frame frame)
    {
        CShell.frame = frame;
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (Throwable error)
    {
        if (error instanceof IncompatibleRemoteServiceException) {
            return _smsgs.xlate("version_mismatch");
        } else if (error instanceof ServiceException) {
            return serverError(error.getMessage());
        } else {
            return _smsgs.xlate("internal_error");
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
            return _smsgs.xlate(error);
        } catch (MissingResourceException e) {
            // looking up a missing translation message throws an exception, yay!
            return "[" + error + "]";
        }
    }

    /**
     * Reports a log message to the console.
     */
    public static void log (String message, Object... args)
    {
        Console.log(message, args); // pass the buck
    }

    /**
     * Gets the client mode given when the outer frame was first loaded.
     */
    public static ClientMode getClientMode ()
    {
        return frame.getEmbedding().mode;
    }

    /**
     * Gets the application id given when the outer frame was first loaded.
     */
    public static int getAppId ()
    {
        return frame.getEmbedding().appId;
    }

    protected static final ServerLookup _smsgs = GWT.create(ServerLookup.class);
}
