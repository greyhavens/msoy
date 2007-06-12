//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.MemberInvites;

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
        setMenu(menuidx++, "Me", CShell.cmsgs.menuMe(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Whirled", "world", "p");
                addLink(menu, "My Home", "world", "m" + creds.getMemberId());
                addLink(menu, "My Profile", "profile", "" + creds.getMemberId());
                addLink(menu, "My Mail", "mail", "");
                menu.addItem("My Account", true, new Command() {
                    public void execute () {
                        CShell.usersvc.getAccountInfo(CShell.ident, new AsyncCallback() {
                            public void onSuccess (Object result) {
                                new EditAccountDialog((AccountInfo) result).show();
                                _popped.hide();
                            }
                            public void onFailure (Throwable cause) {
                                MsoyUI.error(CShell.serverError(cause));
                            }
                        });
                    }
                });
                menu.addItem("Logoff", true, new Command() {
                    public void execute () {
                        _status.logoff();
                        _popped.hide();
                    }
                });
            }
        });

        setMenu(menuidx++, "Places", CShell.cmsgs.menuPlaces(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Whirled", "world", "p");
                addLink(menu, "My Home", "world", "m" + creds.getMemberId());
                MenuBar fmenu = new MenuBar(true);
                FriendEntry[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    for (int ii = 0; ii < friends.length; ii++) {
                        String prefix = (friends[ii].online ? "* " : "");
                        addLink(fmenu, prefix + friends[ii].name + "'s Home", "world",
                                "m" + friends[ii].name.getMemberId());
                    }
                } else {
                    addLink(fmenu, "Go to Your Home first...", "world", "m" + creds.getMemberId());
                }
                menu.addItem("Friends' Homes", fmenu);
                if (CShell.isSupport()) {
                    addLink(menu, "Admin Console", "admin", "");
                }
                // TODO: bank/alchemist
            }
        });

        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                MenuBar fmenu = new MenuBar(true);
                addLink(fmenu, "Search Profiles", "profile", "");
                FriendEntry[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    for (int ii = 0; ii < friends.length; ii++) {
                        addLink(fmenu, (friends[ii].online ? "* " : "") + friends[ii].name,
                                "profile", "" + friends[ii].name.getMemberId());
                    }
                } else {
                    // TODO: add "invite" link if no friends?
                }
                menu.addItem("Profiles", fmenu);
                addLink(menu, "Groups", "group", "");
                addLink(menu, "Forums", "http://forums.whirled.com/");
                menu.addItem("Invitations", true, new Command() {
                    public void execute () {
                        CShell.membersvc.getInvitationsStatus(CShell.ident, new AsyncCallback() {
                            public void onSuccess (Object result) {
                                new SendInvitesDialog((MemberInvites)result).show();
                                _popped.hide();
                            }
                            public void onFailure (Throwable cause) {
                                MsoyUI.error(CShell.serverError(cause));
                            }
                        });
                    }
                });
            }
        });

        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                MenuBar imenu = new MenuBar(true);
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    // TODO: proper i18n
                    addLink(imenu, "My " + CShell.dmsgs.getString("pItemType" + type),
                            "inventory", "" + type);
                }
                menu.addItem("My Stuff", imenu);
                MenuBar cmenu = new MenuBar(true);
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(cmenu, CShell.dmsgs.getString("pItemType" + type),
                            "catalog", "" + type);
                }
                menu.addItem("Catalog", cmenu);
                addLink(menu, "Wiki", "http://wiki.whirled.com/");
                addLink(menu, "Projects", "swiftly", "");
                // TODO: bank/alchemist
            }
        });

        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Games", "inventory", "" + Item.GAME);
                addLink(menu, "Browse Games", "catalog", "" + Item.GAME);
                // TODO: popular games
            }
        });
    }

    /**
     * Called when the player logs off (or navigates to a page and is currently logged off).
     */
    public void didLogoff ()
    {
        int menuidx = 0;
        setMenu(menuidx++, "Me", CShell.cmsgs.menuLogon(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                _status.showLogonPopup(sender.getAbsoluteLeft(), getMenuY(sender));
            }
        });
        setMenu(menuidx++, "Places", CShell.cmsgs.menuPlaces(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Popular Spots", "world", "p");
            }
        });
        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Groups", "group", "");
            }
        });
        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Catalog", "catalog", "");
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Browse", "catalog", "" + Item.GAME);
                // TODO: popular games
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

    protected void addLink (MenuBar menu, String text, final String page, final String args)
    {
        menu.addItem(text, false, new Command() {
            public void execute () {
                History.newItem(Application.createLinkToken(page, args));
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

    protected abstract class MenuPopper implements ClickListener
    {
        public void onClick (Widget sender) {
            if (_popped != null && _popped.isAttached()) {
                _popped.hide();
            }
            MenuBar menu = new MenuBar(true);
            menu.setAutoOpen(true);
            populateMenu(sender, menu);
            _popped = new PopupPanel(true);
            _popped.add(menu);
            _popped.setPopupPosition(sender.getAbsoluteLeft(), getMenuY(sender));
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

        protected abstract void populateMenu (Widget sender, MenuBar menu);
    }

    protected StatusPanel _status;
    protected Label _loglbl, _melbl;

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;
}
