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
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;

import client.util.ServiceUtil;

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
        // if we have no creds token, we are definitely not logged in
        String token = CookieUtil.get(WebCreds.credsCookie());
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
        _usersvc.validateSession(DeploymentConfig.version, token, 1, onValidate);
    }

    /**
     * Call this method if you know we've just logged on and want to let everyone who cares know
     * about it.
     */
    public static void didLogon (SessionData data)
    {
        // store our session information in a cookie
        CookieUtil.set("/", SESSION_DAYS, WebCreds.credsCookie(), data.creds.token);

        // fill in our global creds info
        CShell.creds = data.creds;

        // we're logged in! clear out any leftover cookie, and load up visitor info from the server
        VisitorCookie.clear();
        CShell.visitor = data.visitor;

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
    public static void didLogoff ()
    {
        // clear out our credentials cookie
        CookieUtil.clear("/", WebCreds.credsCookie());

        // clear out our global creds info
        CShell.creds = null;

        // we're logged out, or maybe we're just a guest player.
        // if we don't already have a visitor token, create a brand new shiny one
        if (! VisitorCookie.exists()) {
            VisitorInfo info = new VisitorInfo();
            VisitorCookie.save(info, false);
            CShell.visitor = info;
        } else {
            // we already have one, just load it back in
            CShell.visitor = VisitorCookie.get();
        }

        // let our observers know that we've just logged off
        for (Observer observer : _observers) {
            try {
                observer.didLogoff();
            } catch (Exception e) {
                CShell.log("Observer choked in didLogoff [observer=" + observer + "]", e);
            }
        }
    }

    protected static List<Observer> _observers = new ArrayList<Observer>();

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
