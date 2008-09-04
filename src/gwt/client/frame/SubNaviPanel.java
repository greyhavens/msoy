//
// $Id$

package client.frame;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Frame;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays our sub-navigation.
 */
public class SubNaviPanel extends FlowPanel
{
    public SubNaviPanel (Frame.Tabs tab)
    {
        reset(tab);
    }

    /**
     * Resets the subnavigation to the default for the specified tab.
     */
    public void reset (Frame.Tabs tab)
    {
        clear();

        int memberId = CShell.getMemberId();
        switch (tab) {
        case ME:
            if (CShell.isGuest()) {
                addLink(null, "Home", Pages.LANDING, "");
            } else {
                addLink(null, "Me", Pages.ME, "");
                addImageLink("/images/me/menu_home.png", "Home", Pages.WORLD, "m" + memberId);
                addLink(null, "Rooms", Pages.PEOPLE, Args.compose("rooms", memberId), false);
                addLink(null, "Passport", Pages.ME, "passport");
                addLink(null, "Profile", Pages.PEOPLE, "" + memberId);
                addLink(null, "Mail", Pages.MAIL, "");
                addLink(null, "Account", Pages.ME, "account");
                if (CShell.isSupport()) {
                    addLink(null, "Admin", Pages.ADMINZ, "");
                }
            }
            break;

        case FRIENDS:
            if (CShell.isGuest()) {
                addLink(null, "Search", Pages.PEOPLE, "");
            } else {
                addLink(null, "My Friends", Pages.PEOPLE, "");
                addLink(null, "Invite Friends", Pages.PEOPLE, "invites");
            }
            break;

        case GAMES:
            addLink(null, "Games", Pages.GAMES, "");
            if (!CShell.isGuest()) {
                addLink(null, "My Trophies", Pages.GAMES, Args.compose("t", memberId));
            }
            addLink(null, "All Games", Pages.GAMES, "g");
            break;

        case WHIRLEDS:
            addLink(null, "Whirleds", Pages.WHIRLEDS, "");
            if (!CShell.isGuest()) {
                addLink(null, "My Whirleds", Pages.WHIRLEDS, "mywhirleds");
                addLink(null, "My Discussions", Pages.WHIRLEDS, "unread");
                if (CShell.isSupport()) {
                    addLink(null, "Issues", Pages.WHIRLEDS, "b");
                    addLink(null, "My Issues", Pages.WHIRLEDS, "owned");
                }
            }
            break;

        case SHOP:
            addLink(null, "Shop", Pages.SHOP, "");
            addLink(null, "My Favorites", Pages.SHOP, "f");

            // TODO hiding transactions feature
            if (DeploymentConfig.devDeployment) {
                addLink(null, "Transactions", Pages.SHOP, "t");
            }
            break;

        case HELP:
            addLink(null, "Help", Pages.HELP, "");
            if (CShell.isSupport()) {
                addLink(null, "Admin", Pages.SUPPORT, "admin");
            }
            break;

        default:
            // nada
            break;
        }
    }

    public void addLink (String iconPath, String label, Pages page, String args)
    {
        addLink(iconPath, label, page, args, true);
    }

    public void addLink (String iconPath, String label, Pages page, String args, boolean sep)
    {
        addSeparator(sep);
        if (iconPath != null) {
            add(MsoyUI.createActionImage(iconPath, Link.createListener(page, args)));
            add(new HTML("&nbsp;"));
        }
        add(Link.create(label, page, args));
    }

    public void addContextLink (String label, Pages page, String args)
    {
        insert(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"), 1);
        insert(Link.create(label, page, args), 2);
    }

    public Image addImageLink (String path, String tip, Pages page, String args)
    {
        addSeparator(true);
        Image icon = MsoyUI.createActionImage(path, Link.createListener(page, args));
        icon.setTitle(tip);
        add(icon);
        return icon;
    }

    protected void addSeparator (boolean sep)
    {
        if (getWidgetCount() > 0) {
            add(new HTML("&nbsp;&nbsp;" + (sep ? "|&nbsp;&nbsp;" : "")));
        }
    }
}
