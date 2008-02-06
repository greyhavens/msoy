//
// $Id$

package client.whirled;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import client.shell.Application;
import client.shell.LogonPanel;
import client.shell.Page;
import client.shell.Frame;
import client.util.MsoyUI;

/**
 * Displays a summary of what Whirled is and calls to action.
 */
public class WhatIsTheWhirled extends AbsolutePanel
{
    public WhatIsTheWhirled ()
    {
        setStyleName("whatIsTheWhirled");

        add(MsoyUI.createActionImage("/images/landing/signup.jpg", new ClickListener() {
            public void onClick (Widget widget) {
                Application.go(Page.ACCOUNT, "create");
            }
        }), 540, 152);

        add(MsoyUI.createActionImage("/images/landing/playgames.jpg", new ClickListener() {
            public void onClick (Widget widget) {
                Application.go(Page.WHIRLED, "whirledwide"); // TODO: arcade
            }
        }), 540, 303);

        Button logon = new Button("");
        logon.addStyleName("Logon");
        add(logon, 674, 551);

        add(new LogonPanel(false, logon), 555, 440);
    }
}
