//
// $Id$

package client.frame;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.CShell;
import client.shell.Session;

/**
 * Handles communication with Facebook Connect. See the following site for more information:
 *
 * http://wiki.developers.facebook.com/index.php/Facebook_Connect
 */
public class FBConnect
{
    /**
     * Loads up our Facebook Connect credentials from cookies. Returns null if no creds could be
     * found.
     */
    public static FacebookCreds readCreds ()
    {
        FacebookCreds creds = new FacebookCreds();
        String prefix = DeploymentConfig.facebookKey + "_";
        creds.uid = CookieUtil.get(prefix + "user");
        creds.sessionKey = CookieUtil.get(prefix + "session_key");
        creds.ss = CookieUtil.get(prefix + "ss");
        String expstr = CookieUtil.get(prefix + "expires");
        creds.expires = (expstr.length() > 0) ? Integer.parseInt(expstr) : 0;
        creds.sig = CookieUtil.get(DeploymentConfig.facebookKey);
        return creds.haveAllFields() ? creds : null;
    }

    public FBConnect ()
    {
        Session.addObserver(new Session.Observer() {
            public void didLogon (SessionData data) {
                // nada
            }
            public void didLogoff () {
                if (_initialized) {
                    logoffN();
                }
            }
        });
    }

    /**
     * Initializes Facebook Connect and checks whether or not the user is logged in and "connected"
     * to Facebook. This must be called before any other methods in this class.
     *
     * @param onKnowConnected a callback that will either be provided with the connected user's
     * Facebook UID or 0 if the user is not logged in or "connected".
     */
    public void checkConnected (final AsyncCallback<String> onKnowConnected)
    {
        requireInit(new Command() {
            public void execute () {
                checkConnectedN(onKnowConnected);
            }
        });
    }

    /**
     * Logs the user into Facebook if they are not already.
     *
     * @param onReady called when the user's session is ready supplying their Facebook uid as the
     * argument.
     */
    public void requireSession (final AsyncCallback<String> onReady)
    {
        requireInit(new Command() {
            public void execute () {
                requireSessionN(onReady);
            }
        });
    }

    protected void requireInit (Command onInit)
    {
        if (_initialized) {
            onInit.execute();

        } else {
            // load the Facebook Connect JavaScript client (this should happen asynchronously, but
            // maybe it'll happen synchronously, who knows; it's not like there's a specification for
            // how JavaScript VMs are supposed to behave)
            activateJS("fbhelper", FBHELPER_JS_URL);
            // activateJS("fbconnect", FBCON_JS_URL);
            // so maybe we're already ready to go, or maybe not
            finishInit(onInit);
        }
    }

    protected void finishInit (final Command onInit)
    {
        // if the FB JavaScript has loaded, initN() will return true, otherwise it will return false
        if (initN(DeploymentConfig.facebookKey, onInit)) {
            _initialized = true;
            return;
        }
        CShell.log("Init failed...");

        // in which case we have to try again in 500ms (AFAIK there is no cross-browser way to find
        // out when dynamically loaded JavaScript (that you don't control) is ready)
        new Timer() {
            public void run () {
                CShell.log("Reinitializing FBConnect...");
                finishInit(onInit);
            }
        }.schedule(500);
    }

    protected void activateJS (String ident, String path)
    {
        Element e = DOM.getElementById(ident);
        if (e == null) {
            CShell.log("Missing '" + ident + "' element! We are Le Hosed!");
            return;
        }
        if (DOM.getElementAttribute(e, "src").length() == 0) {
            CShell.log("Starting loading " + path);
            DOM.setElementAttribute(e, "src", path);
        }
    }

    protected native boolean initN (String apiKey, Command onInit) /*-{
        try {
            if (!$wnd.FB_DoInit) {
              return false;
            }
            $wnd.FB_DoInitCallback = function() {
                $wnd.FB.init(apiKey, "/fbconnect.html");
                onInit.@com.google.gwt.user.client.Command::execute()();
            };
            return $wnd.FB_DoInit();
        } catch (e) {
            $wnd.console.log("FBConnect.init failure " + e);
            return false;
        }
    }-*/;

    protected native void checkConnectedN (AsyncCallback<String> onKnowConnected) /*-{
        try {
            $wnd.FB_CheckConnectedCallback = function(uid) {
                onKnowConnected.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(uid);
            };
            $wnd.FB_CheckConnected();
        } catch (e) {
            $wnd.console.log("FBConnect.checkConnected failure " + e);
        }
    }-*/;

    protected native void requireSessionN (AsyncCallback<String> onReady) /*-{
        try {
            $wnd.FB_RequireSessionCallback = function(uid) {
                onReady.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(uid);
            };
            $wnd.FB_RequireSession();
        } catch (e) {
            $wnd.console.log("FBConnect.requireSession failure " + e);
        }
    }-*/;

    protected native void logoffN () /*-{
        try {
            $wnd.FB_Logout();
        } catch (e) {
            $wnd.console.log("FBConnect.logoff failure " + e);
        }
    }-*/;

    protected boolean _initialized;

    protected static final String FBHELPER_JS_URL = "/js/facebook.js";
    protected static final String FBCON_JS_URL =
        "http://static.ak.connect.facebook.com/js/api_lib/v0.4/FeatureLoader.js.php";
}
