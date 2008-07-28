//
// $Id$

package client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import client.shell.BrowserTest;
import client.shell.ShellFrameImpl;

/**
 * Temporary class to handle frame stuff in the old monolithic application format.
 */
public class AppFrameImpl extends ShellFrameImpl
{
    public AppFrameImpl ()
    {
        // default to scrolling off.  In the rare case where a page wants scrolling, it gets enabled
        // explicitly.
        Window.enableScrolling(false);

        // clear out the loading HTML so we can display a browser warning or load Whirled
        DOM.setInnerHTML(RootPanel.get(LOADING_AND_TESTS).getElement(), "");

        // If the browser is unsupported, hide the page (still being built) and show a warning.
        ClickListener continueClicked = new ClickListener() {
            public void onClick (Widget widget) {
                // close the warning and show the page if the visitor choose to continue
                RootPanel.get(LOADING_AND_TESTS).clear();
                RootPanel.get(LOADING_AND_TESTS).setVisible(false);
                RootPanel.get(SITE_CONTAINER).setVisible(true);
            }
        };
        Widget warningDialog = BrowserTest.getWarningDialog(continueClicked);
        if (warningDialog != null) {
            RootPanel.get(SITE_CONTAINER).setVisible(false);
            RootPanel.get(LOADING_AND_TESTS).add(warningDialog);
        } else {
            RootPanel.get(LOADING_AND_TESTS).clear();
            RootPanel.get(LOADING_AND_TESTS).setVisible(false);
        }
    }

    protected static final String SITE_CONTAINER = "ctable";
    protected static final String LOADING_AND_TESTS = "loadingAndTests";
}
