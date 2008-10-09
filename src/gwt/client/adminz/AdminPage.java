//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;

/**
 * Displays an admin dashboard with various server status information and administrative
 * functionality.
 */
public class AdminPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (CShell.isGuest()) {
            setContent(MsoyUI.createLabel(_msgs.indexLogon(), "infoLabel"));

        } else if (!CShell.isSupport()) {
            setContent(MsoyUI.createLabel(_msgs.lackPrivileges(), "infoLabel"));

        } else if (action.equals("browser")) {
            if (_playerBrowser == null) {
                _playerBrowser = new PlayerBrowserPanel();
            }
            setContent(_msgs.browserTitle(), _playerBrowser);
            _playerBrowser.displayPlayersInvitedBy(args.get(1, 0));

        } else if (action.equals("review")) {
            setContent(_msgs.reviewTitle(), new ReviewPanel());

        } else if (action.equals("info")) {
            setContent(_msgs.infoTitle(), new MemberInfoPanel(args.get(1, 0)));

        } else if (action.equals("testlist")) {
            setContent(_msgs.abTestListTitle(), new ABTestListPanel());

        } else if (action.equals("affmap")) {
            setContent(_msgs.affMapTitle(), new AffiliateMapPanel());

        } else if (action.equals("cashout")) {
            setContent(_msgs.cashOutTitle(), new BlingCashOutPanel());

        } else if (action.equals("exchange")) {
            setContent("Exchange", new ExchangePanel());
            
        } else {
            setContent(_msgs.title(), new DashboardPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ADMINZ;
    }

    protected PlayerBrowserPanel _playerBrowser;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
}
