//
// $Id$

package client.frame;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Handles communication with Facebook Connect. See the following site for more information:
 *
 * http://wiki.developers.facebook.com/index.php/Facebook_Connect
 */
public class FBConnect
{
    /**
     * Initializes Facebook Connect.
     *
     * @param onKnowConnected a callback that will either be provided with the connected user's
     * Facebook UID or null if the user is not logged in or "connected".
     */
    public void init (final AsyncCallback<String> onKnowConnected)
    {
        // start our JavaScript loading (this should happen asynchronously, but maybe it'll happen
        // synchronously, who knows; it's not like there's a specification for how JavaScript VMs
        // are supposed to behave)
        Element e = DOM.getElementById("fbconnect");
        if (e != null) {
            DOM.setElementAttribute(e, "src", FBCON_JS_URL);
        }
        // so maybe we're already ready to go, or maybe not
        finishInit(onKnowConnected);
    }

    /**
     * Logs the user into Facebook if they are not already.
     *
     * @param onReady called when the user's session is ready.
     */
    public void requireSession (AsyncCallback<Void> onReady)
    {
        requireSessionN(onReady);
    }

    protected void finishInit (final AsyncCallback<String> onKnowConnected)
    {
        // if the FB JavaScript has loaded, initN() will return true, otherwise it will return false
        if (initN(new AsyncCallback<Void>() {
            public void onSuccess (Void notused) {
                checkConnectedN(onKnowConnected);
            }
            public void onFailure (Throwable t) {
                // not used
            }
        })) {
            return;
        }

        // in which case we have to try again in 500ms (AFAIK there is no cross-browser way to find
        // out when dynamically loaded JavaScript (that you don't control) is ready)
        new Timer() {
            public void run () {
                finishInit(onKnowConnected);
            }
        }.schedule(500);
    }

    protected native boolean initN (AsyncCallback<Void> onInitalized) /*-{
        try {
            FB_RequireFeatures(["Api"], function() {
                onInitialized.onSuccess(null);
            });
            return true;
        } catch (e) {
            return false;
        }
    }-*/;

    protected native void checkConnectedN (AsyncCallback<String> onKnowConnected) /*-{
        FB.Connect.ifUserConnected(function(uid) { onKnowConnected.onSuccess(uid); },
                                   function() { onKnowConnected.onSuccess(null); })
    }-*/;

    protected native void requireSessionN (AsyncCallback<Void> onReady) /*-{
        FB.Connect.requireSession(function() { onReady.onSuccess(null); });
    }-*/;

    protected static final String FBCON_JS_URL =
        "http://static.ak.connect.facebook.com/js/api_lib/v0.4/FeatureLoader.js.php";
}
