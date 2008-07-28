//
// $Id$

package client.shell;

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
    public static TitleBar create (String pageId, ClickListener onClose)
    {
        SubNaviPanel subnavi = new SubNaviPanel();
        int memberId = CShell.getMemberId();

        if (pageId.equals(Page.ME)) {
            subnavi.addLink(null, "Me", Page.ME, "");
            subnavi.addImageLink("/images/me/menu_home.png", "My Home", Page.WORLD,
                "m" + memberId);
            subnavi.addLink(null, "My Rooms", Page.ME, "rooms");
            subnavi.addLink(null, "My Profile", Page.PEOPLE, "" + memberId);
            subnavi.addLink(null, "Mail", Page.MAIL, "");
            subnavi.addLink(null, "Account", Page.ME, "account");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Page.ADMIN, "");
            }

        } else if (pageId.equals(Page.PEOPLE)) {
            if (CShell.isGuest()) {
                subnavi.addLink(null, "Search", Page.PEOPLE, "");
            } else {
                subnavi.addLink(null, "My Friends", Page.PEOPLE, "");
                subnavi.addLink(null, "Invite Friends", Page.PEOPLE, "invites");
            }

        } else if (pageId.equals(Page.GAMES)) {
            subnavi.addLink(null, "Games", Page.GAMES, "");
            if (!CShell.isGuest()) {
                subnavi.addLink(null, "My Trophies", Page.GAMES, Args.compose("t", memberId));
            }
            subnavi.addLink(null, "All Games", Page.GAMES, "g");

        } else if (pageId.equals(Page.WHIRLEDS)) {
            subnavi.addLink(null, "Whirleds", Page.WHIRLEDS, "");
            if (!CShell.isGuest()) {
                subnavi.addLink(null, "My Whirleds", Page.WHIRLEDS, "mywhirleds");
                subnavi.addLink(null, "My Discussions", Page.WHIRLEDS, "unread");
                if (CShell.isSupport()) {
                    subnavi.addLink(null, "Issues", Page.WHIRLEDS, "b");
                    subnavi.addLink(null, "My Issues", Page.WHIRLEDS, "owned");
                }
            }

        } else if (pageId.equals(Page.SHOP)) {
            subnavi.addLink(null, "Shop", Page.SHOP, "");

        } else if (pageId.equals(Page.HELP)) {
            subnavi.addLink(null, "Help", Page.HELP, "");
            if (CShell.isSupport()) {
                subnavi.addLink(null, "Admin", Page.SUPPORT, "admin");
            }
        }

        return new TitleBar(pageId, Page.getDefaultTitle(pageId), subnavi, onClose);
    }

    public TitleBar (String pageId, String title, SubNaviPanel subnavi, ClickListener onClose)
    {
        super("pageTitle", 0, 0);

        setWidget(0, 0, createImage(pageId), 3, null);

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

    protected Image createImage (String page) {
        String id = (page.equals(Page.ME) || page.equals(Page.PEOPLE) ||
                     page.equals(Page.GAMES) || page.equals(Page.WHIRLEDS) ||
                     page.equals(Page.SHOP) || page.equals(Page.HELP)) ? page : "solid";
        return new Image("/images/header/" + id + "_cap.png");
    }

    protected static class SubNaviPanel extends FlowPanel
    {
        public void addLink (String iconPath, String label, final String page, final String args) {
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

        public Image addImageLink (String path, String tip, final String page, final String args) {
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
