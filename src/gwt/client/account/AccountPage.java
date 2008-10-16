//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.shell.CShell;
import client.shell.Page;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays account information.
 */
public class AccountPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("create") && CShell.isGuest()) {
            setContent(_msgs.createTitle(), new CreateAccountPanel());

        } else if (action.equals("logon") && CShell.isGuest()) {
            setContent(_msgs.logonPageTitle(), new LogonPagePanel());

        } else if (action.equals("optout")) {
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            setContent(new ResetPasswordPanel(args));

        } else if (action.equals("welcome")) {
            setContent(_msgs.welcomeTitle(), new WelcomePanel());

        } else if (CShell.isGuest()) {
            setContent(MsoyUI.createLabel(_msgs.indexLogon(), "infoLabel"));
        } else {
            Link.go(Pages.ME, "");
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ACCOUNT;
    }

    protected static final AccountMessages _msgs = GWT.create(AccountMessages.class);
}
