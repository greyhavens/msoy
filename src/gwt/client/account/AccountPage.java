//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.ui.MsoyUI;

/**
 * Displays account information.
 */
public class AccountPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new AccountPage();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("create")) {
            setContent(CAccount.msgs.createTitle(), new CreateAccountPanel());

        } else if (action.equals("login")) {
            setContent(CAccount.msgs.loginPageTitle(), new LoginPagePanel());

        } else if (action.equals("optout")) {
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            setContent(new ResetPasswordPanel(args));

        } else if (action.equals("welcome")) {
            setContent(CAccount.msgs.welcomeTitle(), new WelcomePanel());

        } else if (CAccount.ident == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));
        }
    }

    @Override
    public String getPageId ()
    {
        return ACCOUNT;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CAccount.msgs = (AccountMessages)GWT.create(AccountMessages.class);
    }
}
