//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;

/**
 * Displays our navigation headers.
 */
public class NaviPanel extends FlexTable
{
    public NaviPanel (String page, LogonPanel logon)
    {
        setStyleName("naviPanel");
        setCellPadding(0);
        setCellSpacing(0);

        _logon = logon;
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
                addLink(menu, "/profile/index.html", "Profile");
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/mail/index.html", "Mail");
                menu.addItem("Logoff", true, new Command() {
                    public void execute () {
                        _logon.logoff();
                        _popped.hide();
                    }
                });
                // TODO: account
                // TODO: logout
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Places", CShell.cmsgs.menuPlaces(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/world/index.html#m" + creds.getMemberId(), "My Home");
                addLink(menu, "/world/index.html#nm" + creds.getMemberId(), "My Neighborhood");
                addLink(menu, "/world/index.html#p", "Popular Spots");
                // TODO: bank/alchemist
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
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/catalog/index.html", "Catalog");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, "Games", CShell.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/inventory/index.html#4", "My Games");
                addLink(menu, "/catalog/index.html#4", "Browse");
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
                _logon.showLogonPopup(sender.getAbsoluteLeft(), getMenuY(sender));
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

    protected LogonPanel _logon;

    protected Label _loglbl, _melbl;

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;
}
