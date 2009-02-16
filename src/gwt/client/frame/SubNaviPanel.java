//
// $Id$

package client.frame;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Displays our sub-navigation.
 */
public class SubNaviPanel extends FlowPanel
{
    public SubNaviPanel (Tabs tab)
    {
        reset(tab);
    }

    /**
     * Resets the subnavigation to the default for the specified tab.
     */
    public void reset (Tabs tab)
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
                addLink(null, "My Rooms", Pages.PEOPLE, Args.compose("rooms", memberId));
                if (!CShell.isPermaguest()) {
                    addLink(null, "Friends", Pages.PEOPLE, "");
                    addLink(null, "Account", Pages.ACCOUNT, "edit");
                }
                if (CShell.isSupport()) {
                    addLink(null, "Admin", Pages.ADMINZ, "");
                }
            }
            break;

        case STUFF:
            if (!CShell.isGuest()) {
                addLink(null, "My Stuff", Pages.STUFF, "");
            }
            break;

        case GAMES:
            addLink(null, "Games", Pages.GAMES, "");
            if (!CShell.isGuest()) {
                addLink(null, "My Trophies", Pages.GAMES, Args.compose("t", memberId));
                addLink(null, "My Favorites", Pages.SHOP, Args.compose("f", memberId, 4));
            }
            addLink(null, "New Games", Pages.GAMES, Args.compose("g", -1, 1));
            break;

        case ROOMS:
            addLink(null, "Rooms", Pages.ROOMS, "");
            if (!CShell.isGuest()) {
                addImageLink("/images/me/menu_home.png", "Home", Pages.WORLD, "m" + memberId);
                addLink(null, "My Rooms", Pages.PEOPLE, Args.compose("rooms", memberId));
            }
            break;

        case GROUPS:
            addLink(null, "Groups", Pages.GROUPS, "");
            if (!CShell.isGuest() && !CShell.isPermaguest()) {
                addLink(null, "My Groups", Pages.GROUPS, "mygroups");
                addLink(null, "My Discussions", Pages.GROUPS, "unread");
                if (CShell.isSupport()) {
                    addLink(null, "Issues", Pages.GROUPS, "b");
                    addLink(null, "My Issues", Pages.GROUPS, "owned");
                }
            }
            break;

        case SHOP:
            addLink(null, "Shop", Pages.SHOP, "");
            addLink(null, "My Favorites", Pages.SHOP, "f");
            if (!CShell.isGuest() && !CShell.isPermaguest()) {
                addLink(null, "Transactions", Pages.ME, "transactions");
                addExternalLink("Buy Bars", NaviUtil.onBuyBars(), true);
            }
            break;

        case HELP:
            addLink(null, "Help", Pages.HELP, "");
            addLink(null, "Contact Us", Pages.SUPPORT, "");
            addLink(null, "Report Bug", Pages.GROUPS, Args.compose("f", 72));
            if (CShell.isSupport()) {
                addLink(null, "Admin", Pages.SUPPORT, "admin");
            }
            break;

        default:
            // nada
            break;
        }
    }

    public void addExternalLink (String label, ClickListener listener, boolean sep)
    {
        addSeparator(sep);
        add(MsoyUI.createActionLabel(label, "external", listener));
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

    public void addContextLink (String label, Pages page, String args, int position)
    {
        // sanity check the position
        if (position > getWidgetCount()) {
            position = getWidgetCount();
        }
        insert(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"), position++);
        insert(Link.create(label, page, args), position);
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
