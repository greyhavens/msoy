//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Displays our navigation headers.
 */
public class NaviPanel extends FlexTable
{
    public NaviPanel (StatusPanel status)
    {
        setStyleName("naviPanel");
        setCellPadding(0);
        setCellSpacing(0);
        _status = status;
    }

    /**
     * Called when the player logs on (or navigates to a page and is already logged on).
     */
    public void didLogon (final WebCreds creds)
    {
        // replace our logon menu item with the "me" menu item
        int menuidx = 0;
        setMenu(menuidx++, "Me", CShell.cmsgs.menuMe(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "Profile", "profile", "");
                addLink(menu, "Mail", "mail", "");
                addLink(menu, "Projects", "swiftly", "");
                if (CShell.isSupport()) {
                    addLink(menu, "Admin", "admin", "");
                }
                menu.addItem("Account", true, new Command() {
                    public void execute () {
                        new EditAccountDialog().show();
                        _popped.hide();
                    }
                });
                menu.addItem("Logoff", true, new Command() {
                    public void execute () {
                        _status.logoff();
                        _popped.hide();
                    }
                });
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Places", CShell.cmsgs.menuPlaces(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "My Home", "world", "m" + creds.getMemberId());
                FriendEntry[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    MenuBar fmenu = new MenuBar(true);
                    for (int ii = 0; ii < friends.length; ii++) {
                        String prefix = (friends[ii].online ? "* " : "");
                        addLink(fmenu, prefix + friends[ii].name + "'s Home", "world",
                                "m" + friends[ii].name.getMemberId());
                    }
                    menu.addItem("Friends' Homes", fmenu);
                }
                addLink(menu, "My Neighborhood", "world", "nm" + creds.getMemberId());
                addLink(menu, "Popular Spots", "world", "p");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "Groups", "group", "");
                addLink(menu, "Forums", "http://forums.whirled.com/");
                FriendEntry[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    MenuBar fmenu = new MenuBar(true);
                    for (int ii = 0; ii < friends.length; ii++) {
                        addLink(fmenu, (friends[ii].online ? "* " : "") + friends[ii].name,
                                "profile", "" + friends[ii].name.getMemberId());
                    }
                    menu.addItem("Friends", fmenu);
                } // TODO: add "invite" link if no friends?
                menu.addItem("Invites", true, new Command() {
                    public void execute () {
                        new SendInvitesDialog().show();
                        _popped.hide();
                    }
                });
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "Inventory", "inventory", "");
                addLink(menu, "Catalog", "catalog", "");
                addLink(menu, "Wiki", "http://wiki.whirled.com/");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "My Games", "inventory", "" + Item.GAME);
                addLink(menu, "Browse", "catalog", "" + Item.GAME);
                // TODO: popular games
                popupMenu(sender, menu);
            }
        });
    }

    /**
     * Called when the player logs off (or navigates to a page and is currently logged off).
     */
    public void didLogoff ()
    {
        int menuidx = 0;
        setMenu(menuidx++, "Me", CShell.cmsgs.menuLogon(), new ClickListener() {
            public void onClick (Widget sender) {
                _status.showLogonPopup(sender.getAbsoluteLeft(), getMenuY(sender));
            }
        });
        setMenu(menuidx++, "Places", CShell.cmsgs.menuPlaces(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "Popular Spots", "world", "p");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "Groups", "group", "");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "Catalog", "catalog", "");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "Browse", "catalog", "" + Item.GAME);
                // TODO: popular games
                popupMenu(sender, menu);
            }
        });
    }

    protected void setMenu (int menuidx, String ident, String text, ClickListener listener)
    {
        VerticalPanel box = new VerticalPanel();
        box.add(MsoyUI.createCustomActionLabel("", "Button", listener));
        box.add(MsoyUI.createCustomActionLabel(text, "Link", listener));
        setWidget(0, menuidx, box);
        getFlexCellFormatter().setStyleName(0, menuidx, ident);
    }

    protected void addLink (MenuBar menu, String text, String page, String args)
    {
        menu.addItem(Application.createLinkHtml(text, page, args), true, new Command() {
            public void execute () {
                _popped.hide();
            }
        });
    }

    protected void addLink (MenuBar menu, String text, String href)
    {
        menu.addItem("<a href=\"" + href + "\">" + text + "</a>", true, new Command() {
            public void execute () {
                _popped.hide();
            }
        });
    }

    protected void popupMenu (Widget from, MenuBar menu)
    {
        _popped = new PopupPanel(true);
        _popped.add(menu);
        _popped.setPopupPosition(from.getAbsoluteLeft(), getMenuY(from));
        _popped.show();
    }

    protected int getMenuY (Widget from)
    {
        int height = from.getAbsoluteTop() + from.getOffsetHeight();
        if (((Label)from).getText().equals("")) { // doris the hackasaurus!
            height += 15;
        }
        return height;
    }

    protected StatusPanel _status;

    protected Label _loglbl, _melbl;

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;
}
