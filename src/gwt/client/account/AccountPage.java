//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;
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
        if (action.equals("create") && CAccount.isGuest()) {
            setContent(CAccount.msgs.createTitle(), new CreateAccountPanel());

        } else if (action.equals("login") && CAccount.isGuest()) {
            setContent(CAccount.msgs.loginPageTitle(), new LoginPagePanel());

        } else if (action.equals("optout")) {
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            setContent(new ResetPasswordPanel(args));

        } else if (action.equals("welcome")) {
            setContent(CAccount.msgs.welcomeTitle(), new WelcomePanel());

        } else if (CAccount.isGuest()) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));
        } else {
            Link.go(Pages.ME, "");
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ACCOUNT;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CAccount.msgs = (AccountMessages)GWT.create(AccountMessages.class);
    }
}
