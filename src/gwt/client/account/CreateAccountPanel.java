//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.CShell;
import client.shell.FullLogonPanel;
import client.ui.MsoyUI;
import client.ui.RegisterPanel;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends FlowPanel
{
    /**
     * Creates a new account creation panel in the given mode.
     */
    public CreateAccountPanel ()
    {
        setStyleName("createAccount");

        final FlowPanel content = MsoyUI.createFlowPanel("Content");
        if (CShell.isPermaguest()) {
            content.add(MsoyUI.createLabel(_msgs.createIntro(), "Intro"));
            content.add(MsoyUI.createLabel(_msgs.createSaveModeIntro(), "Coins"));
            content.add(new RegisterPanel());
            content.add(MsoyUI.createLabel(_msgs.createLogon(), "Intro"));
            content.add(new FullLogonPanel());
        } else {
            content.add(MsoyUI.createLabel(_msgs.createLogon(), "Intro"));
            content.add(new FullLogonPanel());
            content.add(MsoyUI.createLabel(_msgs.createIntro(), "Intro"));
            content.add(MsoyUI.createLabel(_msgs.createCoins(), "Coins"));
            content.add(new RegisterPanel());
        }

        add(WidgetUtil.makeShim(15, 15));
        add(new Image("/images/account/create_bg_top.png"));
        add(content);
        add(new Image("/images/account/create_bg_bot.png"));
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
}
