//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.util.NoopAsyncCallback;
import client.util.ServiceUtil;

/**
 * A central place where we keep track of whether or not we've logged on or logged off.
 */
public class Session
{
    public enum LogoffCondition
    {
        /** Player hit the 'logoff' button. */
        LOGOFF_REQUESTED,
        /** Player tried to log on, and failed. */
        LOGON_ATTEMPT_FAILED,
        /** Visitor showed up without a credentials cookie - probably a new visitor. */
        NO_CREDENTIALS;
    }

    /** An interface used for observing logon and logoff. */
    public static interface Observer {
        /** Called when we have just logged on. */
        void didLogon (SessionData data);

        /** Called when we have just logged off. */
        void didLogoff ();
    }

    /** The lifespan of our session cookie. */
    public static final int SESSION_DAYS = 7;

    /**
     * Registers to be notified when we logon or logoff.
     */
    public static void addObserver (Observer observer)
    {
        _observers.add(observer);
    }

    /**
     * Removes an observer registration.
     */
    public static void removeObserver (Observer observer)
    {
        _observers.remove(observer);
    }

    /**
     * Confirms that our existing credentials are still valid. This results in a call to either
     * {@link #didLogon} or {@link #didLogoff} when the validity of our credentials are known.
     */
    public static void validate ()
    {
        validate(CookieUtil.get(WebCreds.credsCookie()));
    }

    /**
     * Assigns our session token to the value obtained from the server by flash and re-validates it
     * without disturbing the current state of the Flash client.
     */
    public static void conveyLoginFromFlash (String token)
    {
        validate(token);
    }

    /**
     * Call this method if you know we've just logged on and want to let everyone who cares know
     * about it.
     */
    public static void didLogon (SessionData data)
    {
        // store our session information in a cookie
        setSessionCookie(data.creds.token);

        // fill in our global creds info
        CShell.creds = data.creds;

        // we're logged in! clear out any leftover cookie, and load up visitor info from the server
        VisitorCookie.clear();
        EntryVectorCookie.clear();
        CShell.visitor = data.visitor;

        // tell it from the mountain
        _membersvc.trackSessionStatusChange(data.visitor, false, false, new NoopAsyncCallback());

        // let our observers know that we've just logged on
        for (Observer observer : _observers) {
            try {
                observer.didLogon(data);
            } catch (Exception e) {
                CShell.log("Observer choked in didLogon [observer=" + observer + "]", e);
            }
        }
    }

    /**
     * Call this method if you know we've just logged off and want to let everyone who cares know
     * about it.
     */
    public static void didLogoff (LogoffCondition condition)
    {
        // clear out our credentials cookie
        CookieUtil.clear("/", WebCreds.credsCookie());

        // clear out our global creds info
        CShell.creds = null;

        // we're logged out, or maybe we're just a guest player.
        // if we don't already have a visitor token, create a brand new shiny one
        boolean newInfo = false;
        if (! VisitorCookie.exists()) {
            VisitorInfo info = new VisitorInfo();
            VisitorCookie.save(info, false);
            CShell.visitor = info;
            _membersvc.trackVisitorInfoCreation(info, new NoopAsyncCallback());
            newInfo = true;
        } else {
            // we already have one, just load it back in
            CShell.visitor = VisitorCookie.get();
        }

        // log me
        boolean guest = (condition == LogoffCondition.NO_CREDENTIALS);
        _membersvc.trackSessionStatusChange(
            CShell.visitor, guest, newInfo, new NoopAsyncCallback());

        // let our observers know that we've just logged off
        for (Observer observer : _observers) {
            try {
                observer.didLogoff();
            } catch (Exception e) {
                CShell.log("Observer choked in didLogoff [observer=" + observer + "]", e);
            }
        }
    }

    /**
     * Reconfigures our role based on whether our email is now or is no longer validated.
     */
    public static void updateEmailValid (boolean validated)
    {
        if (CShell.creds == null) {
            return; // bogosity!
        }
        if (validated) {
            switch (CShell.creds.role) {
            case PERMAGUEST: break; // bogosity!
            case REGISTERED:
                CShell.creds.role = WebCreds.Role.VALIDATED;
                break;
            case VALIDATED:
            case SUPPORT:
            case ADMIN:
            case MAINTAINER:
                break; // leave them as is
            }

        } else {
            switch (CShell.creds.role) {
            case PERMAGUEST: break; // bogosity!
            case REGISTERED: break; // bogosity!
            case VALIDATED:
                CShell.creds.role = WebCreds.Role.REGISTERED;
                break;
            case SUPPORT:
            case ADMIN:
            case MAINTAINER:
                break; // leave them as is
            }
        }
    }

    protected static void setSessionCookie (String token)
    {
        CookieUtil.set("/", SESSION_DAYS, WebCreds.credsCookie(), token);
    }

    protected static void validate (String token)
    {
        // if we have no creds token, we are definitely not logged in
        if (token == null) {
            // defer execution of didLogoff so that the caller sees the same behavior in both
            // situations: immediate return of this method and a call to didLogon or didLogoff at
            // some later time after the current call stack has completed
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    didLogoff(LogoffCondition.NO_CREDENTIALS);
                }
            });
            return;
        }

        // if we do have a creds token, we need to check with the server to see if it has expired
        AsyncCallback<SessionData> onValidate = new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    didLogoff(LogoffCondition.LOGON_ATTEMPT_FAILED);
                } else {
                    didLogon(data);
                }
            }
            public void onFailure (Throwable t) {
                didLogoff(LogoffCondition.LOGON_ATTEMPT_FAILED);
            }
        };
        _usersvc.validateSession(DeploymentConfig.version, token, 1, onValidate);
    }

    protected static List<Observer> _observers = new ArrayList<Observer>();

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
