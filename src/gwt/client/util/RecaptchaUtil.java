//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;

/**
 * Utility methods for using RECAPTCHA.
 */
public class RecaptchaUtil
{
    /**
     * Returns true if the RECAPTCHA system is enabled.
     */
    public static boolean isEnabled ()
    {
        return !DeploymentConfig.recaptchaPublic.equals("");
    }

    /**
     * Creates the special DIV that will contain the captcha UI.
     */
    public static HTML createDiv (String elemId)
    {
        return new HTML("<div id=\"" + elemId + "\"></div>");
    }

    /**
     * Initializes the RECAPTCHA system, placing the captcha in the element on the page with the
     * specified id.
     */
    public static void init (String elemId)
    {
        if (!isEnabled()) {
            return;
        }

        Element e = DOM.getElementById("recaptcha");
        if (e == null) {
            CShell.log("Zoiks! Requested RECAPTCHA but have no 'recaptcha' <script> tag.");
            return;
        }

        // stuff our public key into the window so that their JS can find it
        initKey(DeploymentConfig.recaptchaPublic);

        // start the captcha JS loading
        DOM.setElementAttribute(e, "src", "http://www.google.com/recaptcha/api/js/recaptcha_ajax.js");

        // display a little "loading" indicator
        RootPanel.get(elemId).add(MsoyUI.createLabel(_cmsgs.captchaLoading(), "label"));

        // start a timer that will initialize the captcha bits once our async JS is loaded
        initCaptcha(elemId);
    }

    public static native String getChallenge () /*-{
        return $wnd.Recaptcha.get_challenge();
    }-*/;

    public static native String getResponse () /*-{
        return $wnd.Recaptcha.get_response();
    }-*/;

    public static native void reload () /*-{
        $wnd.Recaptcha.reload();
    }-*/;

    public static native void focus () /*-{
        $wnd.Recaptcha.focus_response_field();
    }-*/;

    protected static void initCaptcha (final String elemId)
    {
        // our JavaScript is loaded asynchrnously, so there's a possibility that it won't be set up
        // by the time we try to initialize ourselves; in that case we have no recourse but to try
        // again in a short while (there's no way to find out when async JS is loaded)
        if (!create(elemId)) {
            new Timer() {
                public void run () {
                    initCaptcha(elemId);
                }
            }.schedule(500);
        }
    }

    protected static native void initKey (String recaptchaPublic) /*-{
        $wnd.recaptchaPublicKey = recaptchaPublic;
    }-*/;

    protected static native boolean create (String elemId) /*-{
        try {
            if ($wnd.Recaptcha != null) {
                $wnd.Recaptcha.create($wnd.recaptchaPublicKey, elemId, { theme: "white" });
                return true;
            }
        } catch (e) {
            // fall through, return false
        }
        return false;
    }-*/;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
