//
// $Id$

package client.frame;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.BillingUtil;
import client.util.Link;

/**
 * Displays our sub-navigation.
 */
public class SubNaviPanel extends FlowPanel
{
    /**
     * Creates subnav for the given tab.
     */
    public SubNaviPanel (Tabs tab)
    {
        reset(tab);
    }

    /**
     * Creates subnav for being in a game or in the world.
     */
    public SubNaviPanel ()
    {
        reset(null);
    }

    /**
     * Resets the subnavigation to the default for the specified tab. If tab is null, create sub
     * navigation for either being in a scene or being in a game according to the specified value.
     */
    public void reset (Tabs tab)
    {
        clear();

        int memberId = CShell.getMemberId();
        if (tab == null) {
            return;
        }
        switch (tab) {
        case ME:
            if (CShell.isGuest()) {
                addLink(null, "Home", Pages.LANDING);
            } else {
                addImageLink("/images/me/menu_home.png", "Home",
                             Pages.WORLD, Args.compose("m" + memberId));
                if (!CShell.getClientMode().isMinimal()) {
                    addLink(null, "My Rooms", Pages.PEOPLE, "rooms", memberId);
                }
                if (CShell.isRegistered()) {
                    addLink(null, "Friends", Pages.PEOPLE);
                    addLink(null, "Account", Pages.ACCOUNT, "edit");
                }
                if (CShell.isSupport()) {
                    addLink(null, "Admin", Pages.ADMINZ);
                }
            }
            break;

        case BILLING:
            addImageLink("/images/billing/menu_lock.png",
                "Billing pages are https secure, click here to open in a new window",
                new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        BillingUtil.popBillingPage("");
                    }
                }, false);
            addLink(null, "Get Bars", false, Pages.BILLING);
            if (CShell.isRegistered()) {
                addLink(null, "My Bars", Pages.ME, Args.compose("transactions", "2"));
            }
            addExternalLink("http://wiki.whirled.com/Billing_FAQ", "Billing FAQ", true);
            if (CShell.isSupport()) {
                addLink(null, "Billing Admin", Pages.BILLING, "admin");
            }
            break;

        case STUFF:
            break;

        case GAMES:
            if (CShell.isMember()) {
                addLink(null, "My Trophies", Pages.GAMES, "t", memberId);
                addLink(null, "My Games", Pages.EDGAMES, "m");
            }
            addLink(null, "New Games", Pages.GAMES, "g", -2, "newest"); // -2 is all, ugh
            if (CShell.isSupport()) {
                addLink(null, "Edit Arcades", Pages.EDGAMES, "ea");
            }
            break;

        case ROOMS:
            if (!CShell.getClientMode().isMinimal() && CShell.isMember()) {
                addImageLink("/images/me/menu_home.png", "Home",
                             Pages.WORLD, Args.compose("m" + memberId));
                addLink(null, "My Rooms", Pages.PEOPLE, "rooms", memberId);
            }
            break;

        case GROUPS:
            if (CShell.isRegistered()) {
                addLink(null, "My Groups", Pages.GROUPS, "mygroups");
                addLink(null, "My Discussions", Pages.GROUPS, "unread");
                if (CShell.isSupport()) {
                    addLink(null, "Issues", Pages.ISSUES);
                    addLink(null, "My Issues", Pages.ISSUES, "mine");
                }
            }
            break;

        case SHOP:
            addLink(null, "My Favorites", Pages.SHOP, "f");
            if (CShell.isRegistered()) {
                addLink(null, "Transactions", Pages.ME, "transactions");
                addLink(null, "Get Bars", Pages.BILLING);
            }
            break;

        case HELP:
            addLink(null, "Contact Us", Pages.SUPPORT);
            addLink(null, "Report Bug", Pages.GROUPS, "f", 72);
            if (CShell.isSupport()) {
                addLink(null, "Admin", Pages.SUPPORT, "admin");
            }
            break;

        default:
            // nada
            break;
        }
    }

    public void addLink (String iconPath, String label, Pages page, Object... args)
    {
        addLink(iconPath, label, true, page, args);
    }

    public void addLink (String iconPath, String label, boolean sep, Pages page, Object... args)
    {
        addSeparator(sep);
        if (iconPath != null) {
            add(MsoyUI.createActionImage(iconPath, Link.createHandler(page, args)));
            add(new HTML("&nbsp;"));
        }
        add(Link.create(label, page, args));
    }

    public void addExternalLink (String url, String label, boolean sep)
    {
        addSeparator(sep);
        add(MsoyUI.createExternalAnchor(url, label));
    }

    public void addContextLink (String label, Pages page, Args args, int position)
    {
        // sanity check the position
        if (position > getWidgetCount()) {
            position = getWidgetCount();
        }
        insert(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"), position++);
        insert(Link.create(label, page, args), position);
    }

    public Image addImageLink (String path, String tip, Pages page, Args args)
    {
        return addImageLink(path, tip, Link.createHandler(page, args), true);
    }

    public Image addImageLink (String path, String tip, ClickHandler clickHandler, boolean sep)
    {
        addSeparator(sep);
        Image icon = MsoyUI.createActionImage(path, clickHandler);
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

    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class);
}
