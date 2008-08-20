//
// $Id$

package client.frame;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Frame;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a page title and subnavigation at the top of the page content area.
 */
public class TitleBar extends SmartTable
{
    /**
     * Creates a title bar for the specified page.
     */
    public static TitleBar create (Frame.Tabs tab, ClickListener onClose)
    {
        SubNaviPanel subnavi = new SubNaviPanel();
        int memberId = CShell.getMemberId();

        switch (tab) {
        case ME:
            if (CShell.isGuest()) {
                subnavi.addLink(null, "Home", Pages.LANDING, "");
            } else {
                subnavi.addLink(null, "Me", Pages.ME, "");
                subnavi.addImageLink("/images/me/menu_home.png", "Home", Pages.WORLD,
                                     "m" + memberId);
                subnavi.addLink(null, "Passport", Pages.ME, "passport");
                subnavi.addLink(null, "Rooms", Pages.ME, "rooms");
                subnavi.addLink(null, "Profile", Pages.PEOPLE, "" + memberId);
                subnavi.addLink(null, "Mail", Pages.MAIL, "");
                subnavi.addLink(null, "Account", Pages.ME, "account");
                if (CShell.isSupport()) {
                    subnavi.addLink(null, "Admin", Pages.ADMINZ, "");
                }
            }
            break;

        case FRIENDS:
            if (CShell.isGuest()) {
                subnavi.addLink(null, "Search", Pages.PEOPLE, "");
            } else {
                subnavi.addLink(null, "My Friends", Pages.PEOPLE, "");
                subnavi.addLink(null, "Invite Friends", Pages.PEOPLE, "invites");
            }
            break;

        case GAMES:
            subnavi.addLink(null, "Games", Pages.GAMES, "");
            if (!CShell.isGuest()) {
                subnavi.addLink(null, "My Trophies", Pages.GAMES, Args.compose("t", memberId));
            }
            subnavi.addLink(null, "All Games", Pages.GAMES, "g");
            break;

        case WHIRLEDS:
            subnavi.addLink(null, "Whirleds", Pages.WHIRLEDS, "");
            if (!CShell.isGuest()) {
                subnavi.addLink(null, "My Whirleds", Pages.WHIRLEDS, "mywhirleds");
                subnavi.addLink(null, "My Discussions", Pages.WHIRLEDS, "unread");
                if (CShell.isSupport()) {
                    subnavi.addLink(null, "Issues", Pages.WHIRLEDS, "b");
                    subnavi.addLink(null, "My Issues", Pages.WHIRLEDS, "owned");
                }
            }
            break;

        case SHOP:
            subnavi.addLink(null, "Shop", Pages.SHOP, "");
            // TODO hiding favorites feature
            if (DeploymentConfig.devDeployment) {
                subnavi.addLink(null, "My Favorites", Pages.SHOP, "f");
            }
            break;

        case HELP:
            subnavi.addLink(null, "Help", Pages.HELP, "");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Pages.SUPPORT, "admin");
            }
            break;

        default:
            // nada
            break;
        }

        return new TitleBar(tab, Page.getDefaultTitle(tab), subnavi, onClose);
    }

    public TitleBar (Frame.Tabs tab, String title, SubNaviPanel subnavi, ClickListener onClose)
    {
        super("pageTitle", 0, 0);

        setWidget(0, 0, createImage(tab), 3, null);

        _titleLabel = new Label(title);
        _titleLabel.setStyleName("Title");

        Widget back = MsoyUI.createImageButton("backButton", new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        });

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(WidgetUtil.makeShim(10, 1));
        panel.add(back);
        panel.add(WidgetUtil.makeShim(12, 1));
        panel.add(_titleLabel);
        panel.setCellVerticalAlignment(back, HorizontalPanel.ALIGN_MIDDLE);

        setWidget(1, 0, panel);
        setWidget(1, 2, _subnavi = subnavi, 1, "SubNavi");
        getFlexCellFormatter().setVerticalAlignment(1, 1, HasAlignment.ALIGN_BOTTOM);

        _closeBox = MsoyUI.createActionImage("/images/ui/close.png", onClose);
        _closeBox.addStyleName("Close");
        _closeShim = MsoyUI.createHTML("&nbsp;", "Close");
        setCloseVisible(false);
    }

    public void setTitle (String title) {
        if (title != null) {
            _titleLabel.setText(title);
        }
    }

    public void setCloseVisible (boolean visible) {
        _subnavi.remove(_closeBox);
        _subnavi.remove(_closeShim);
        if (visible) {
            _subnavi.add(_closeBox);
        } else {
            _subnavi.add(_closeShim);
        }
    }

    protected Image createImage (Frame.Tabs tab) {
        String id = (tab == null) ? "solid" : tab.toString().toLowerCase();
        return new Image("/images/header/" + id + "_cap.png");
    }

    protected static class SubNaviPanel extends FlowPanel
    {
        public void addLink (String iconPath, String label, final Pages page, final String args) {
            addSeparator();
            if (iconPath != null) {
                add(MsoyUI.createActionImage(iconPath, new ClickListener() {
                    public void onClick (Widget sender) {
                        Link.go(page, args);
                    }
                }));
                add(new HTML("&nbsp;"));
            }
            add(Link.create(label, page, args));
        }

        public Image addImageLink (String path, String tip, final Pages page, final String args) {
            addSeparator();
            Image icon = MsoyUI.createActionImage(path, new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(page, args);
                }
            });
            icon.setTitle(tip);
            add(icon);
            return icon;
        }

        protected void addSeparator () {
            if (getWidgetCount() > 0) {
                add(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"));
            }
        }
    }

    protected Label _titleLabel;
    protected SubNaviPanel _subnavi;
    protected Widget _closeBox, _closeShim;
}
