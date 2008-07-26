//
// $Id$

package client.admin;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays an admin dashboard with various server status information and administrative
 * functionality.
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
        if (CAdmin.ident == null) {
            setContent(MsoyUI.createLabel(CAdmin.msgs.indexLogon(), "infoLabel"));

        } else if (!CAdmin.isSupport()) {
            setContent(MsoyUI.createLabel(CAdmin.msgs.lackPrivileges(), "infoLabel"));

        } else if (args.get(0, "").equals("browser")) {
            if (_playerBrowser == null) {
                _playerBrowser = new PlayerBrowserPanel();
            }
            setContent(CAdmin.msgs.browserTitle(), _playerBrowser);
            _playerBrowser.displayPlayersInvitedBy(args.get(1, 0));

        } else if (args.get(0, "").equals("review")) {
            setContent(CAdmin.msgs.reviewTitle(), new ReviewPanel());

        } else if (args.get(0, "").equals("info")) {
            setContent(CAdmin.msgs.infoTitle(), new MemberInfoPanel(args.get(1, 0)));

        } else if (args.get(0, "").equals("testlist")) {
            setContent(CAdmin.msgs.abTestListTitle(), new ABTestListPanel());

        } else {
            setContent(CAdmin.msgs.title(), new DashboardPanel());
        }
    }

    @Override // from Page
    protected String getPageId ()
    {
        return ADMIN;
    }

    @Override // from Page
    protected String getTabPageId ()
    {
        return ME;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CAdmin.msgs = (AdminMessages)GWT.create(AdminMessages.class);
    }

    protected PlayerBrowserPanel _playerBrowser;
}
