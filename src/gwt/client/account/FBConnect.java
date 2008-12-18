//
// $Id$

package client.account;

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
        // TODO: this ain't gonna work no more
        Session.addObserver(new Session.Observer() {
            public void didLogon (SessionData data) {
                // nada
            }
            public void didLogoff () {
                logoff();
            }
        });
    }

    /**
     * Logs the user into Facebook if they are not already.
     *
     * @param onReady called when the user's session is ready supplying their Facebook uid as the
     * argument.
     */
    public native void requireSession (AsyncCallback<String> onReady) /*-{
        try {
            $wnd.FB_RequireSessionCallback = function(uid) {
                onReady.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(uid);
            };
            $wnd.FB_RequireSession();
        } catch (e) {
            $wnd.console.log("FBConnect.requireSession failure " + e);
        }
    }-*/;

    protected native void logoff () /*-{
        try {
            $wnd.FB_Logout();
        } catch (e) {
            $wnd.console.log("FBConnect.logoff failure " + e);
        }
    }-*/;
}
