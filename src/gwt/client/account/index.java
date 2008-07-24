//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.data.WebCreds;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays account information.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        _onLogonPage = null;

        String action = args.get(0, "");
        if (action.equals("create")) {
            _onLogonPage = ME;
            _onLogonArgs = "";
            setContent(CAccount.msgs.createTitle(),
                new CreateAccountPanel(new CreateAccountPanel.RegisterListener() {
                    public void didRegister () {
                        _onLogonPage = ME;
                        _onLogonArgs = "";
                    }
                }));

        } else if (action.equals("login")) {
            _onLogonPage = ME;
            _onLogonArgs = "";
            setContent(CAccount.msgs.loginPageTitle(), new LoginPagePanel());

        } else if (action.equals("optout")) {
            _onLogonPage = ME;
            _onLogonArgs = "";
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            _onLogonPage = ME;
            _onLogonArgs = "";
            setContent(new ResetPasswordPanel(args));

        } else if (action.equals("welcome")) {
            setContent(CAccount.msgs.welcomeTitle(), new WelcomePanel());

        } else if (CAccount.ident == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));
        }
    }

    @Override // from Page
    protected void didLogon (WebCreds creds)
    {
        if (_onLogonPage != null) {
            Application.go(_onLogonPage, _onLogonArgs);
        } else {
            super.didLogon(creds);
        }
    }

    @Override // from Page
    protected String getPageId ()
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

    protected String _onLogonPage, _onLogonArgs;
}
