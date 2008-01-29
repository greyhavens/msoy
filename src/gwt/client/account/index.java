//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.MsoyCallback;
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
        if (action.equals("i")) {
            _onLogonRedirect = true;
            CAccount.membersvc.getInvitation(args.get(1, ""), true, new MsoyCallback() {
                public void onSuccess (Object result) {
                    displayInvitation((Invitation)result);
                }
            });

        } else if (action.equals("create")) {
            _onLogonRedirect = true;
            setContent(new CreateAccountPanel(args.get(1, "")));

        } else if (action.equals("invites")) {
            setContent(new SendInvitesPanel());

        } else if (action.equals("optout")) {
            _onLogonRedirect = true;
            setContent(new OptOutPanel(args.get(1, "")));

        } else if (action.equals("resetpw")) {
            _onLogonRedirect = true;
            setContent(new ResetPasswordPanel(args));

        } else if (CAccount.ident == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.indexLogon(), "infoLabel"));

        } else {
            setContent(new EditAccountPanel());
        }
    }

    // @Override // from Page
    protected void didLogon (WebCreds creds)
    {
        if (_onLogonRedirect) {
            Application.go(Page.WHIRLED, "");
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

    protected void displayInvitation (Invitation invite)
    {
        if (CAccount.getMemberId() != 0) {
            // we should do this before looking up the invitation, but this code is cleaner
            setContent(MsoyUI.createLabel(CAccount.msgs.inviteLogout(), "infoLabel"));
        } else if (invite == null) {
            setContent(MsoyUI.createLabel(CAccount.msgs.inviteMissing(), "infoLabel"));
        } else {
            WorldClient.displayFlash(
                "memberHome=" + invite.inviter.getMemberId() + "&invite=" + invite.inviteId);
        }
    }

    protected boolean _onLogonRedirect;
}
