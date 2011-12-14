//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.CookieUtil;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * Determines whether the visitor is using a suppported browser and displays a
 * warning specific to their detected browser if they are not.
 */
public class BrowserTest
{
    /**
     * If the visitor's browser is not supported, return a warning message widget.
     * If the browser is supported, return null.
     */
    public static Widget getWarningDialog (ClickHandler continueClicked)
    {
        boolean facebook = CShell.getClientMode().isFacebook();

        // TEMP: clear old cookie
        CookieUtil.clear("/", "BrowserTest_seen");
        // if they already have a cookie, don't show a message
        String cookie = CookieUtil.get(TEST_SEEN_COOKIE);

        if (cookie != null && cookie.equals("true")) {
            return null;
        }

        String message;
        String agent = getUserAgent().toLowerCase();
        // old MSIE
        if (agent.contains("msie 6.0")) {
            // facebook or not, this is a very old browser at this point
            message = _msgs.browserOldMsie();

        // MSIE 8 works alright until further notice
        } else if (agent.contains("msie 8")) {
            message = null;

        // not 6 or 8, must be 7?
        } else if (agent.contains("msie")) {
            message = null;

        // safari/chrome - passable now that cookies are supported
        } else if (agent.contains("webkit")) {
            message = null;

        // opera
        } else if (agent.contains("opera")) {
            message = facebook ? null : _msgs.browserUnsupported();

        // lump all gecko browsers into firefox
        } else if (agent.contains("gecko")) {
            // this is a little weird, but we're trying to parse this with limited GWT regex supp.
//            if (!agent.contains(" rv:") || agent.contains(" rv:0") ||
//                    (agent.contains(" rv:1") && !agent.contains(" rv:1.9"))) {
//                // TODO implement once the number of ff2 users is under 10%
//                //message = _msgs.browserOldFirefox();
//                message = null;
//            } else {
                message = null;
//            }

        // all else
        } else {
            message = facebook ? null : _msgs.browserUnsupported();
        }

        FlowPanel browserTestWidget = null;
        if (message != null) {
            browserTestWidget = MsoyUI.createFlowPanel("browserTest");
            FlowPanel messageBox = MsoyUI.createFlowPanel("Message");
            browserTestWidget.add(messageBox);
            messageBox.add(MsoyUI.createLabel(_msgs.browserTitle(), "Title"));
            messageBox.add(MsoyUI.createHTML(message, null));

            ClickHandler getFF = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    Window.open("http://getfirefox.com", "_blank", "");
                }
            };
            messageBox.add(MsoyUI.createActionImage("/images/landing/get_firefox_button.png",
                                                    _msgs.browserGetFirefox(), getFF));
            messageBox.add(MsoyUI.createActionImage("/images/landing/continue_button.png",
                                                    _msgs.browserClose(), continueClicked));
        }

        // set a cookie so they don't see this warning again
        CookieUtil.set("/", 365, TEST_SEEN_COOKIE, "true", null);

        return browserTestWidget;
    }

    /**
     * Return the browser useragent.
     */
    private static native String getUserAgent () /*-{
        return navigator.userAgent;
    }-*/;

    protected static final FrameMessages _msgs = GWT.create(FrameMessages.class);

    protected static final String TEST_SEEN_COOKIE = "BrowserTest_seen2";
}
