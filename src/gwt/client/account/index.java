//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.data.Invitation;

import client.shell.Args;
import client.shell.Frame;
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
        if (args.get(0, "").equals("invite")) {
            setContent(new InvitationPanel(this, args.get(1, "")));

        } else if (args.get(0, "").equals("invites")) {
            setContent(new SendInvitesPanel());

        } else if (args.get(0, "").equals("create")) {
            showCreateAccount(null);

        } else if (args.get(0, "").equals("edit")) {
            setContent(new EditAccountPanel());

        } else if (args.get(0, "").equals("optout")) {
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (CAccount.ident == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));

        } else {
            // TODO: display account info page
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

    protected void showCreateAccount (Invitation invite)
    {
        setContent(new CreateAccountPanel(invite));
    }
}
