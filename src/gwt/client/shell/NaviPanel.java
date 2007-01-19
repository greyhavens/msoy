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
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.MsoyUI;

/**
 * Displays our navigation headers.
 */
public class NaviPanel extends FlexTable
{
    public NaviPanel (ShellContext ctx, String page, LogonPanel logon)
    {
        setStyleName("naviPanel");
        setCellPadding(0);
        setCellSpacing(0);

        _ctx = ctx;
        _logon = logon;
    }

    /**
     * Called when the player logs on (or navigates to a page and is already logged on.
     */
    public void didLogon (final WebCreds creds)
    {
        // replace our logon menu item with the "me" menu item
        int menuidx = 0;
        setMenu(menuidx++, _ctx.cmsgs.menuMe(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/profile/index.html", "Profile");
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/mail/index.html", "Mail");
                // TODO: account
                // TODO: logout
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuPlaces(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/world/index.html#m" + creds.getMemberId(), "My Home");
                addLink(menu, "/world/index.html#nm" + creds.getMemberId(), "My Neighborhood");
                addLink(menu, "/world/index.html#p", "Popular Spots");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/group/index.html", "Groups");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/inventory/index.html", "Inventory");
                addLink(menu, "/catalog/index.html", "Catalog");
                // TODO: bank/alchemist
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuGames(), new ClickListener() {
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
     * Called when the player logs off.
     */
    public void didLogoff ()
    {
        int menuidx = 0;
        setMenu(menuidx++, _ctx.cmsgs.menuLogon(), new ClickListener() {
            public void onClick (Widget sender) {
                _logon.showLogonPopup();
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuPlaces(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/world/index.html#p", "Popular Spots");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuPeople(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/group/index.html", "Groups");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuStuff(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/catalog/index.html", "Catalog");
                popupMenu(sender, menu);
            }
        });
        setMenu(menuidx++, _ctx.cmsgs.menuGames(), new ClickListener() {
            public void onClick (Widget sender) {
                MenuBar menu = new MenuBar(true);
                addLink(menu, "/catalog/index.html#4", "Browse");
                // TODO: popular games
                popupMenu(sender, menu);
            }
        });
    }

    protected void setMenu (int menuidx, String text, ClickListener listener)
    {
        setMenu(menuidx, MsoyUI.createPlainActionLabel(text, listener));
    }

    protected void setMenu (int menuidx, Label label)
    {
        int column = menuidx*3;
        getFlexCellFormatter().setStyleName(0, column++, "Left");
        getFlexCellFormatter().setStyleName(0, column, "Link");
        setWidget(0, column++, label);
        getFlexCellFormatter().setStyleName(0, column++, "Right");
    }

    protected void addLink (MenuBar menu, String href, String text)
    {
        menu.addItem("<a href=\"" + href + "\">" + text + "</a>", true, (Command)null);
    }

    protected void popupMenu (Widget from, MenuBar menu)
    {
        PopupPanel popup = new PopupPanel(true);
        popup.add(menu);
        popup.setPopupPosition(from.getAbsoluteLeft() - 6,
                               from.getAbsoluteTop() + from.getOffsetHeight() + 3);
        popup.show();
    }

    protected ShellContext _ctx;
    protected LogonPanel _logon;

    protected Label _loglbl, _melbl;
}
