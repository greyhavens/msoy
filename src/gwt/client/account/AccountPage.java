//
// $Id$

package client.account;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.shell.Page;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays account information.
 */
public class AccountPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("create") && (CShell.isGuest() || CShell.isPermaguest())) {
            setContent(_msgs.createTitle(), new CreateAccountPanel());

        } else if (action.equals("edit")) {
            if (CShell.isMember()) {
                setContent(_msgs.editTitle(), new EditAccountPanel());
            } else {
                Link.go(Pages.ACCOUNT, "create"); // guests/permaguests have to register first
            }

        } else if (action.equals("optout")) {
            setContent(new OptOutPanel(args.get(1, ""), args.get(2, 0)));

        } else if (action.equals("resetpw")) {
            setContent(new ResetPasswordPanel(args));

        } else if (action.equals("logon") || CShell.isGuest()) {
            setContent(_msgs.logonPageTitle(), new LogonPagePanel());

        } else if (action.equals("welcome")) {
            setContent(_msgs.welcomeTitle(), new WelcomePanel());

        } else if (action.equals("v")) {
            final int memberId = args.get(1, 0);
            final String code = args.get(2, "");
            _usersvc.validateEmail(memberId, code, new MsoyCallback<Boolean>() {
                public void onSuccess (Boolean valid) {
                    String msg; 
                    if (valid) {
                        msg = _msgs.emailValidated();
                        if (memberId == CShell.getMemberId()) {
                            CShell.frame.updateValidated(true);
                        }
                    } else {
                        msg = _msgs.emailInvalid();
                    }
                    setContent(_msgs.editTitle(), MsoyUI.createLabel(msg, "infoLabel"));
                }
            });

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
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
