//
// $Id$

package client.shell;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.web.gwt.ExternalSiteId;
import com.threerings.msoy.web.gwt.FacebookCreds;
import com.threerings.msoy.web.gwt.SessionData;

import client.shell.CShell;
import client.shell.Session;
import client.util.InfoCallback;

/**
 * Handles communication with Facebook Connect. WARNING: This references window.top for convenient
 * use it inner frames, but means it will break if you try to use it in a canvas app.
 */
public class FBConnect
{
    /**
     * Loads up our Facebook Connect credentials from cookies. Returns null if no creds could be
     * found.
     *
     * DANGER: Do not call unless you're sure fbhelper has loaded! Use waitFor()
     */
    public static FacebookCreds readCreds ()
    {
        FacebookCreds creds = new FacebookCreds();
        // TODO: gah, we'll need the key & site id of the default connect app here
        creds.site = ExternalSiteId.facebookApp(CShell.getAppId());

        creds.uid = getUserId();
        creds.accessToken = getAccessToken();
        creds.signedRequest = getSignedRequest();

        return creds.haveAllFields() ? creds : null;
    }

    public FBConnect ()
    {
        // TODO: this ain't gonna work no more
        Session.addObserver(new Session.Observer() {
            public void didLogon (SessionData data) {
                // nada
            }
            public void didLogoff () {
                waitFor(new InfoCallback<Void>() {
                    public void onSuccess (Void unused) {
                        logoff();
                    }
                });
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
        waitFor(new AsyncCallback<Void>() {
            public void onSuccess (Void unused) {
                getSession(onReady);
            }
            public void onFailure (Throwable t) {
                onReady.onFailure(t);
            }
        });
    }

    protected native void getSession (AsyncCallback<String> onReady) /*-{
        try {
            $wnd.top.FB_RequireSessionCallback = function (uid) {
                onReady.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(uid);
            };
            $wnd.top.FB_RequireSession();
        } catch (e) {
            $wnd.console.log("FBConnect.requireSession failure " + e);
        }
    }-*/;

    protected static native String getAccessToken () /*-{
        return $wnd.top.FB_LastResponse().accessToken;
    }-*/;

    protected static native String getSignedRequest () /*-{
        return $wnd.top.FB_LastResponse().signedRequest;
    }-*/;

    protected static native String getUserId () /*-{
        return $wnd.top.FB_LastResponse().userID;
    }-*/;

    protected native void logoff () /*-{
        try {
            $wnd.top.FB_Logout();
        } catch (e) {
            $wnd.console.log("FBConnect.logoff failure " + e);
        }
    }-*/;

    /**
     * Gets the FB app ID.
     *
     * DANGER: Do not call unless you're sure fbhelper has loaded! Use waitFor()
     */
    public static native String getKey () /*-{
        return $wnd.top.FB_GetKey();
    }-*/;

    /**
     * Calls onReady when the fbhelper script has finished loading.
     */
    public static native void waitFor (AsyncCallback<Void> onReady) /*-{
        var onload = function () {
            onReady.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)();
        };

        if ("FB_GetKey" in $wnd.top) {
            onload();

        } else {
            var fbhelper = $wnd.top.document.getElementById("fbhelper");
            if (fbhelper.addEventListener) {
                fbhelper.addEventListener("load", function listener () {
                    fbhelper.removeEventListener("load", listener, false);
                    onload();
                }, false);
            } else if (fbhelper.attachEvent) {
                // Legacy IE ludicrousy
                fbhelper.attachEvent("onload", onload);
            }
        }
    }-*/;
}
