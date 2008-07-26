//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.CommentServiceAsync;
import com.threerings.msoy.web.client.ItemServiceAsync;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;

import client.editem.EditemMessages;
import client.item.ItemMessages;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Use this to make service calls. */
    public static WebIdent ident;

    /** This member's current level. */
    public static int level;

    /** Provides comment-related service. */
    public static CommentServiceAsync commentsvc;

    /** Provides item-related services. */
    public static ItemServiceAsync itemsvc;

    /** Provides catalog-related services. */
    public static CatalogServiceAsync catalogsvc;

    /** Messages used by the item interfaces. */
    public static ItemMessages imsgs;

    /** Messages used by the editor interfaces. */
    public static EditemMessages emsgs;

    /** Messages that must be looked up dynamically. */
    public static DynamicMessages dmsgs;

    /** Contains translations for server-supplied messages. */
    public static ServerMessages smsgs;

    /** The application that's running. */
    public static Application app;

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
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (Throwable error)
    {
        if (error instanceof ServiceException) {
            return serverError(error.getMessage());
        } else {
            return smsgs.getString("internal_error");
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
            return smsgs.getString(error);
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
}
