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

    // @Override from Page
    public void onHistoryChanged (Args args)
    {
        _onLogonRedirect = false;

        String action = args.get(0, "");
        if (action.equals("create")) {
            _onLogonRedirect = true;
            setContent(new CreateAccountPanel(), false);

        } else if (action.equals("optout")) {
            _onLogonRedirect = true;
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            _onLogonRedirect = true;
            setContent(new ResetPasswordPanel(args));

        } else if (CAccount.ident == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));
        }
    }

    // @Override // from Page
    protected void didLogon (WebCreds creds)
    {
        if (_onLogonRedirect) {
            Application.go(Page.ME, "");
        } else {
            super.didLogon(creds);
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return ACCOUNT;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CAccount.msgs = (AccountMessages)GWT.create(AccountMessages.class);
    }

    protected boolean _onLogonRedirect;
}
