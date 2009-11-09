//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.util.NoopAsyncCallback;
import client.util.events.StatusChangeEvent;
import client.util.events.ThemeChangeEvent;

/**
 * A central place where we keep track of whether or not we've logged on or logged off.
 */
public class Session
{
    /** An interface used for observing logon and logoff. */
    public static interface Observer {
        /** Called when we have just logged on. */
        void didLogon (SessionData data);

        /** Called when we have just logged off. */
        void didLogoff ();
    }

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

        // clear out any old visitor cookie, and use the data from the server
        VisitorCookie.clear();
        _visitor = data.visitor;

        // let our observers know that we've just logged on
        for (Observer observer : _observers) {
            try {
                observer.didLogon(data);
            } catch (Exception e) {
                CShell.log("Observer choked in didLogon [observer=" + observer + "]", e);
            }
        }

        // repeat the session data values
        Frame frame = CShell.frame;
        frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.MAIL, data.newMailCount));
        frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.COINS, data.flow));
        frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.BARS, data.gold));
        frame.dispatchEvent(new StatusChangeEvent(StatusChangeEvent.LEVEL, data.level));

        frame.dispatchEvent(new ThemeChangeEvent(
            (data.theme != null) ? data.theme.getGroupId() : 0));
    }

    /**
     * Call this method if you know we've just logged off and want to let everyone who cares know
     * about it.
     */
    public static void didLogoff ()
    {
        // clear out our credentials cookie
        CookieUtil.clear("/", WebCreds.credsCookie());

        // clear out our global creds info
        CShell.creds = null;

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
     * Called when our email address and/or validation status changes.
     */
    public static void emailUpdated (String address, boolean validated)
    {
        if (CShell.creds == null) {
            return; // bogosity!
        }

        // update our cached credentials
        CShell.creds.accountName = address;
        CShell.creds.validated = validated;
    }

    /**
     * Provides our visitor info to the {@link Frame}. Don't call this method, call {@link
     * Frame#getVisitorInfo}.
     */
    public static VisitorInfo frameGetVisitorInfo ()
    {
        if (_visitor != null) {
            return _visitor;
        } else if (VisitorCookie.exists()) {
            return (_visitor = VisitorCookie.get());
        } else {
            VisitorCookie.save(_visitor = new VisitorInfo(), false);
            String vector = StringUtil.getOr(History.getToken(), Pages.LANDING.makeToken());
            _membersvc.noteNewVisitor(_visitor, vector, new NoopAsyncCallback());
            return _visitor;
        }
    }

    protected static void setSessionCookie (String token)
    {
        CookieUtil.set("/", WebUserService.SESSION_DAYS, WebCreds.credsCookie(), token);
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
                    didLogoff();
                }
            });
            return;
        }

        // if we do have a creds token, we need to check with the server to see if it has expired
        AsyncCallback<SessionData> onValidate = new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    didLogoff();
                } else {
                    didLogon(data);
                }
            }
            public void onFailure (Throwable t) {
                didLogoff();
            }
        };

        _usersvc.validateSession(
            DeploymentConfig.version, token, 1, CShell.getAppId(), onValidate);
    }

    protected static VisitorInfo _visitor;
    protected static List<Observer> _observers = new ArrayList<Observer>();

    protected static final WebUserServiceAsync _usersvc = GWT.create(WebUserService.class);
    protected static final WebMemberServiceAsync _membersvc = GWT.create(WebMemberService.class);
}
