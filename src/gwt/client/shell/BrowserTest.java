//
// $Id$

package client.shell;

import client.ui.MsoyUI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.util.CookieUtil;

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
    public static Widget getWarningDialog (ClickListener continueClicked)
    {
        // if they already have a cookie, don't show a message
        String cookie = CookieUtil.get(TEST_SEEN_COOKIE);

        if (cookie != null && cookie.equals("true")) {
            return null;
        }

        String browserType = getBrowserType();

        final String message;

        if (browserType.equals(SUPPORTED_MSIE) || browserType.equals(SUPPORTED_FIREFOX)) {
            message = null;
        }
        else if (browserType.equals(SAFARI)) {
            message = _cmsgs.browserUnsupported();
        }
        else if (browserType.equals(OLD_MSIE)) {
            message = _cmsgs.browserOldMsie();
        }
        else if (browserType.equals(OLD_FIREFOX)) {
            // TODO implement once the number of ff2 users is under 10%
            //message = _cmsgs.browserOldFirefox();
            message = null;
        }
        else {
            message = _cmsgs.browserUnsupported();
        }

        FlowPanel browserTestWidget = null;
        if (message != null) {
            browserTestWidget = MsoyUI.createFlowPanel("browserTest");
            FlowPanel messageBox = MsoyUI.createFlowPanel("Message");
            browserTestWidget.add(messageBox);
            messageBox.add(MsoyUI.createLabel(_cmsgs.browserTitle(), "Title"));
            messageBox.add(MsoyUI.createHTML(message, null));

            ClickListener getFF = new ClickListener() {
                public void onClick (Widget widget) {
                    Window.open("http://getfirefox.com", "_blank", "");
                }
            };
            messageBox.add(MsoyUI.createActionImage("/images/landing/get_firefox_button.png",
                                                    _cmsgs.browserGetFirefox(), getFF));
            messageBox.add(MsoyUI.createActionImage("/images/landing/continue_button.png",
                                                    _cmsgs.browserClose(), continueClicked));
        }

        // set a cookie so they don't see this warning again
        CookieUtil.set("/", 365, TEST_SEEN_COOKIE, "true");

        return browserTestWidget;
    }

    /**
     * Determine which browser is being used.  Native javascript will return one of:
     * old_msie, supported_msie(7.0+), old_firefox, supported_firefox(3.0+), safari, opera, unknown.
     */
    private static native String getBrowserType() /*-{
            var ua = navigator.userAgent.toLowerCase();

            if (ua.indexOf("msie 6.0") != -1) {
                return "old_msie";
            }

            // assume anything that isn't ie6 is newer
            else if (ua.indexOf("msie") != -1) {
                return "supported_msie";
            }

            else if (ua.indexOf("webkit") != -1) {
                return "safari";
            }

            else if (ua.indexOf("opera") != -1) {
                return "opera";
            }

            // lump all gecko browsers into firefox
            else if (ua.indexOf("gecko") != -1) {
                var result = /rv:([0-9]+)\.([0-9]+)/.exec(ua);
                if (result && result.length == 3) {
                    var version = (parseInt(result[1]) * 10) + parseInt(result[2]);
                    if (version >= 19)
                       return "supported_firefox";
                }
                return "old_firefox";
            }

            return "unknown";
    }-*/;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String OLD_MSIE = "old_msie";
    protected static final String SUPPORTED_MSIE = "supported_msie";
    protected static final String OLD_FIREFOX = "old_firefox";
    protected static final String SUPPORTED_FIREFOX = "supported_firefox";
    protected static final String SAFARI = "safari";
    protected static final String OPERA = "opera";
    protected static final String UNKNOWN = "unknown";

    protected static final String TEST_SEEN_COOKIE = "BrowserTest_seen";
}
