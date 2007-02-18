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

import com.threerings.msoy.web.data.FriendInfo;
import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Displays our navigation headers.
 */
public class NaviPanel extends FlexTable
{
    public NaviPanel (String page, StatusPanel status)
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
                addLink(menu, "/profile/index.html", "Profile");
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/mail/index.html", "Mail");
                if (CShell.creds.isSupport) {
                    addLink(menu, "/admin/index.html", "Admin");
                }
                // TODO: account
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
                addLink(menu, "/world/index.html#m" + creds.getMemberId(), "My Home");
                FriendInfo[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    MenuBar fmenu = new MenuBar();
                    for (int ii = 0; ii < friends.length; ii++) {
                        addLink(fmenu, "/world/index.html#m" + friends[ii].name.getMemberId(),
                                (friends[ii].online ? "* " : "") + friends[ii].name + "'s Home");
                    }
                    menu.addItem("Friends' Homes", fmenu);
                }
                addLink(menu, "/world/index.html#nm" + creds.getMemberId(), "My Neighborhood");
                addLink(menu, "/world/index.html#p", "Popular Spots");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "/group/index.html", "Groups");
                FriendInfo[] friends = FlashClients.getFriends();
                if (friends.length > 0) {
                    MenuBar fmenu = new MenuBar();
                    for (int ii = 0; ii < friends.length; ii++) {
                        addLink(fmenu, "/profile/index.html#" + friends[ii].name.getMemberId(),
                                (friends[ii].online ? "* " : "") + friends[ii].name);
                    }
                    menu.addItem("Friends", fmenu);
                } // TODO: add "invite" link if no friends?
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/catalog/index.html", "Catalog");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                menu.setAutoOpen(true);
                addLink(menu, "/inventory/index.html#4", "My Games");
                addLink(menu, "/catalog/index.html#4", "Browse");
                // TODO: popular games
                popupMenu(sender, menu);
            }
        });
        setWidget(0, menuidx++, _menuFrame = new MenuFrame());
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
                addLink(menu, "/world/index.html#p", "Popular Spots");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/group/index.html", "Groups");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Stuff", CShell.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/catalog/index.html", "Catalog");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/catalog/index.html#4", "Browse");
                // TODO: popular games
                popupMenu(sender, menu);
            }
        });
        setWidget(0, menuidx++, _menuFrame = new MenuFrame());
    }

    protected void setMenu (int menuidx, String ident, String text, ClickListener listener)
    {
        VerticalPanel box = new VerticalPanel();
        box.add(MsoyUI.createCustomActionLabel("", "Button", listener));
        box.add(MsoyUI.createCustomActionLabel(text, "Link", listener));
        setWidget(0, menuidx, box);
        getFlexCellFormatter().setStyleName(0, menuidx, ident);
    }

    protected void addLink (MenuBar menu, String href, String text)
    {
        menu.addItem("<a href=\"" + href + "\">" + text + "</a>", true, new Command() {
            public void execute () {
                _popped.hide();
            }
        });
    }

    protected void popupMenu (Widget from, MenuBar menu)
    {
        _popped = new PopupPanel(true) {
            public void hide () {
                super.hide();
                _menuFrame.hide();
            }
            // PopupPanel doesn't call hide() when it autoHides - it calls a private method, so I
            // can't override it
            public boolean onEventPreview(Event event) {
                if (DOM.eventGetType(event) == Event.ONCLICK &&
                    // these are the circumstances under which PopupPanel will autoHide
                    !DOM.isOrHasChild(getElement(), DOM.eventGetTarget(event))) {
                    _menuFrame.hide();
                }
                return super.onEventPreview(event);
            }
        };
        _popped.add(menu);
        _popped.setPopupPosition(from.getAbsoluteLeft(), getMenuY(from));
        _popped.show();
        DOM.setStyleAttribute(_popped.getElement(), "zIndex", "2000");
        _menuFrame.showAt(_popped.getAbsoluteLeft(), _popped.getAbsoluteTop(), 
                          _popped.getOffsetWidth(), _popped.getOffsetHeight());
    }

    protected int getMenuY (Widget from)
    {
        int height = from.getAbsoluteTop() + from.getOffsetHeight();
        if (((Label)from).getText().equals("")) { // doris the hackasaurus!
            height += 15;
        }
        return height;
    }

    /**
     * This is a hack to let the menu pop up over Flash on Firefox on Linux properly.
     */
    protected class MenuFrame extends Widget 
    {
        public MenuFrame ()
        {
            super();
            _iframe = DOM.createElement("iframe");
            DOM.setStyleAttribute(_iframe, "display", "none");
            DOM.setStyleAttribute(_iframe, "position", "absolute");
            // the z-index of the PopupPanel is set to 2000
            DOM.setStyleAttribute(_iframe, "zIndex", "1999");
            DOM.setStyleAttribute(_iframe, "border", "none");
            setElement(_iframe);
        }

        public void hide () 
        {
            DOM.setStyleAttribute(_iframe, "display", "none");
        }

        public void showAt (int x, int y, int width, int height)
        {
            DOM.setStyleAttribute(_iframe, "left", x + "px");
            DOM.setStyleAttribute(_iframe, "top", y + "px");
            DOM.setStyleAttribute(_iframe, "width", width + "px");
            DOM.setStyleAttribute(_iframe, "height", height + "px");
            DOM.setStyleAttribute(_iframe, "display", "block");
        }

        protected Element _iframe;
    }

    protected StatusPanel _status;

    protected Label _loglbl, _melbl;

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;

    /** The iframe used in the hack to make these menus work over Flash in Linux. */
    protected MenuFrame _menuFrame;
}
