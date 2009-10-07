//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;

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
public class FrameHeader extends SmartTable
    implements Session.Observer
{
    public FrameHeader (ClickHandler onLogoClick)
    {
        super("frameHeader", 0, 0);

        setWidth("100%");

        _logo = MsoyUI.createActionImage(DEFAULT_LOGO_URL, onLogoClick);
        FlowPanel logoHolder = MsoyUI.createFlowPanel("Logo", _logo);
        setWidget(0, 0, logoHolder);

        int col = 1;
        addButton(col++, Pages.ME, _cmsgs.menuMe(), _images.me(), _images.ome(), _images.sme());
        // TODO: remove the tab images
        addButton(col++, Pages.STUFF, _cmsgs.menuStuff(), _images.stuff(), _images.ostuff(),
                  _images.sstuff());
        addButton(col++, Pages.GAMES, _cmsgs.menuGames(), _images.games(), _images.ogames(),
                  _images.sgames());
        addButton(col++, Pages.ROOMS, _cmsgs.menuRooms(), _images.rooms(), _images.orooms(),
                  _images.srooms());
        addButton(col++, Pages.GROUPS, _cmsgs.menuWorlds(), _images.worlds(), _images.oworlds(),
                  _images.sworlds());
        addButton(col++, Pages.SHOP, _cmsgs.menuShop(), _images.shop(), _images.oshop(),
                  _images.sshop());
        _statusCol = col;

        // listen for session state changes
        Session.addObserver(this);
    }

    /**
     * Replace the Whirled logo with the URL identified by the given path, or restore the
     * default if path is null.
     */
    public void setLogoUrl (String path)
    {
        _logo.setUrl(path != null ? path : DEFAULT_LOGO_URL);
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
        getFlexCellFormatter().setHorizontalAlignment(0, _statusCol, HasAlignment.ALIGN_RIGHT);
        getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_TOP);
        setWidget(0, _statusCol, _status, 1, "Right");
    }

    // from Session.Observer
    public void didLogoff ()
    {
        getFlexCellFormatter().setHorizontalAlignment(0, _statusCol, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_TOP);
        setWidget(0, _statusCol, new SignOrLogonPanel(), 1, "Right");
    }

    /**
     * Go to the next invite promo text, if any.
     */
    public void tickPromo ()
    {
        if (_status.isVisible()) {
            _status.tickPromo();
        }
    }

    protected void addButton (int col, Pages page, String text, AbstractImagePrototype up,
                              AbstractImagePrototype over, AbstractImagePrototype down) {
        NaviButton button = new NaviButton(page, text, up, over, down);
        setWidget(0, col, button);
        _buttons.add(button);
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

    protected static class SignOrLogonPanel extends SmartTable
    {
        public SignOrLogonPanel () {
            super(0, 0);
            PushButton signup = new PushButton(_cmsgs.headerSignup(), NaviUtil.onMustRegister());
            signup.setStyleName("SignupButton");
            signup.addStyleName("Button");
            setWidget(0, 0, signup);
            getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
            setWidget(0, 1, WidgetUtil.makeShim(10, 10));
            PushButton logon = new PushButton(_cmsgs.headerLogon(), NaviUtil.onMustRegister());
            logon.setStyleName("LogonButton");
            logon.addStyleName("Button");
            setWidget(0, 2, logon);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
    }

    protected int _statusCol;
    protected List<NaviButton> _buttons = new ArrayList<NaviButton>();
    protected StatusPanel _status = new StatusPanel();
    protected Image _logo;

    protected static final NaviImages _images = GWT.create(NaviImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);

    protected static final String DEFAULT_LOGO_URL = "/images/header/header_logo.png";
}
