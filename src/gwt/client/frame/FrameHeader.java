//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.Tabs;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.images.navi.NaviImages;
import client.shell.CShell;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Displays our navigation buttons, member status and/or logon/signup buttons.
 */
public class FrameHeader
    implements Session.Observer
{
    public FrameHeader (ClickHandler onLogoClick)
    {
        _logoContainer = MsoyUI.createSimplePanel(null, "frameHeaderLogo");

        _statusContainer = MsoyUI.createSimplePanel(null, "frameHeaderStatus");

        _naviPanel = new SmartTable("frameHeaderNavi", 0, 0);

        updateTabs();

        // listen for session state changes
        Session.addObserver(this);
    }

    /**
     * Determine whether a page (tab) should be hidden or not. By default, all our tabs show.
     */
    public boolean setHidden (Pages page, boolean hidden)
    {
        boolean changed;
        if (hidden) {
            changed = _hidden.add(page);
        } else {
            changed = _hidden.remove(page);
        }
        if (changed) {
            updateTabs();
        }
        return changed;
    }

    /**
     * Sets the visibility of all components of the frame header.
     */
    public void setVisible (boolean vis)
    {
        _naviPanel.setVisible(vis);
        _statusContainer.setVisible(vis);
        _logoContainer.setVisible(vis);
    }

    /**
     * Returns our top-navigation panel (tabs).
     */
    public Widget getNaviPanel ()
    {
        return _naviPanel;
    }

    /**
     * Returns our status panel.
     */
    public Widget getStatusPanel ()
    {
        return _statusContainer;
    }

    /**
     * Returns our logo.
     */
    public Widget getLogo ()
    {
        return _logoContainer;
    }

    public void selectTab (Tabs tab)
    {
        for (NaviButton button : _buttons) {
            button.setSelected(button.page.getTab() == tab);
        }
    }

    // from Session.Observer
    public void didLogon (SessionData data)
    {
        _statusContainer.setWidget(_status);
    }

    // from Session.Observer
    public void didLogoff ()
    {
        _statusContainer.setWidget(_logonPanel);
    }

    protected void updateTabs ()
    {
        _naviPanel.clear();

        int col = 0;
        col += addButton(col, Pages.ME, _cmsgs.menuMe(), _images.me(), _images.ome(), _images.sme());
        // TODO: remove the tab images
        col += addButton(col, Pages.STUFF, _cmsgs.menuStuff(), _images.stuff(), _images.ostuff(),
                  _images.sstuff());
        col += addButton(col, Pages.GAMES, _cmsgs.menuGames(), _images.games(), _images.ogames(),
                  _images.sgames());
        col += addButton(col, Pages.ROOMS, _cmsgs.menuRooms(), _images.rooms(), _images.orooms(),
                  _images.srooms());
        col += addButton(col, Pages.GROUPS, _cmsgs.menuWorlds(), _images.worlds(), _images.oworlds(),
                  _images.sworlds());
        col += addButton(col, Pages.SHOP, _cmsgs.menuShop(), _images.shop(), _images.oshop(),
                  _images.sshop());
    }

    protected int addButton (int col, Pages page, String text, AbstractImagePrototype up,
                              AbstractImagePrototype over, AbstractImagePrototype down)
    {
        NaviButton button = new NaviButton(page, text, up, over, down);
        _buttons.add(button);
        if (_hidden.contains(page)) {
            return 0;
        }
        _naviPanel.setWidget(0, col, button);
        return 1;
    }

    protected static class NaviButton extends SimplePanel
    {
        public final Pages page;

        public NaviButton (Pages page, String text, AbstractImagePrototype up,
                           AbstractImagePrototype over, AbstractImagePrototype down) {
            setStyleName("NaviButton");
            this.page = page;

            setWidget(MsoyUI.createActionLabel(text, new ClickHandler() {
                public void onClick (ClickEvent event) {
                    // if a guest clicks on "me", send them to create account
                    if (NaviButton.this.page == Pages.ME && CShell.isGuest()) {
                        NaviUtil.onMustRegister().onClick(null);
                    } else {
                        Link.go(NaviButton.this.page, "");
                    }
                }
            }));
        }

        public void setSelected (boolean selected)
        {
            String selStyle = "NaviButtonSelected";
            if (selected) {
                addStyleName(selStyle);
            } else {
                removeStyleName(selStyle);
            }
        }
    }

    protected static SmartTable makeLogonPanel ()
    {
        SmartTable panel = new SmartTable(0, 0);
        PushButton signup = new PushButton(_cmsgs.headerSignup(), NaviUtil.onMustRegister());
        signup.setStyleName("SignupButton");
        signup.addStyleName("Button");
        panel.setWidget(0, 0, signup);
        panel.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        panel.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        PushButton logon = new PushButton(_cmsgs.headerLogon(), NaviUtil.onMustRegister());
        logon.setStyleName("LogonButton");
        logon.addStyleName("Button");
        panel.setWidget(0, 2, logon);
        panel.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        return panel;
    }

    protected SmartTable _naviPanel;
    protected SimplePanel _statusContainer;
    protected SimplePanel _logoContainer;
    protected StatusPanel _status = new StatusPanel();
    protected SmartTable _logonPanel = makeLogonPanel();
    protected int _statusCol;
    protected List<NaviButton> _buttons = new ArrayList<NaviButton>();
    protected Set<Pages> _hidden = new HashSet<Pages>();

    protected static final NaviImages _images = GWT.create(NaviImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
