//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.Pages;
import com.threerings.msoy.web.client.Tabs;
import com.threerings.msoy.web.data.SessionData;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.images.navi.NaviImages;
import client.shell.CShell;
import client.shell.LogonPanel;
import client.shell.Session;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays our navigation buttons, member status and/or logon/signup buttons.
 */
public class FrameHeader extends SmartTable
    implements Session.Observer
{
    public FrameHeader (ClickListener onLogoClick)
    {
        super("frameHeader", 0, 0);
        setWidth("100%");
        int col = 0;

        String lpath = "/images/header/header_logo.png";
        setWidget(col++, 0, MsoyUI.createActionImage(lpath, onLogoClick), 1, "Logo");
        addButton(col++, Pages.ME, _cmsgs.menuMe(), _images.me(), _images.ome(), _images.sme());
        addButton(col++, Pages.STUFF, _cmsgs.menuStuff(), _images.stuff(), _images.ostuff(),
                  _images.sstuff());
        addButton(col++, Pages.GAMES, _cmsgs.menuGames(), _images.games(), _images.ogames(),
                  _images.sgames());
        addButton(col++, Pages.WHIRLEDS, _cmsgs.menuWorlds(), _images.worlds(), _images.oworlds(),
                  _images.sworlds());
        addButton(col++, Pages.SHOP, _cmsgs.menuShop(), _images.shop(), _images.oshop(),
                  _images.sshop());
        addButton(col++, Pages.HELP, _cmsgs.menuHelp(), _images.help(), _images.ohelp(),
                  _images.shelp());
        _statusCol = col;

        // listen for session state changes
        Session.addObserver(this);
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
        getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_BOTTOM);
        setWidget(0, _statusCol, _status, 1, "Right");
    }

    // from Session.Observer
    public void didLogoff ()
    {
        getFlexCellFormatter().setHorizontalAlignment(0, _statusCol, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(0, _statusCol, HasAlignment.ALIGN_TOP);
        setWidget(0, _statusCol, new SignOrLogonPanel(), 1, "Right");
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

            _upImage = up.createImage();
            _upImage.addStyleName("actionLabel");
            _upImage.addMouseListener(new MouseListenerAdapter() {
                public void onMouseEnter (Widget sender) {
                    setWidget(_overImage);
                }
            });

            _overImage = over.createImage();
            _overImage.addStyleName("actionLabel");
            _overImage.addMouseListener(new MouseListenerAdapter() {
                public void onMouseLeave (Widget sender) {
                    setWidget(_upImage);
                }
            });
            ClickListener go = new ClickListener() {
                public void onClick (Widget sender) {
                    // if a guest clicks on "me", send them to create account
                    if (NaviButton.this.page == Pages.ME && CShell.isGuest()) {
                        Link.go(Pages.ACCOUNT, "create");
                    } else {
                        Link.go(NaviButton.this.page, "");
                    }
                }
            };
            _overImage.addClickListener(go);

            _downImage = down.createImage();
            _downImage.addStyleName("actionLabel");
            _downImage.addClickListener(go);

            setWidget(_upImage);
        }

        public void setSelected (boolean selected)
        {
            setWidget(selected ? _downImage : _upImage);
        }

        protected Image _upImage, _overImage, _downImage;
    }

    protected static class SignOrLogonPanel extends SmartTable
    {
        public SignOrLogonPanel () {
            super(0, 0);
            PushButton signup = new PushButton(
                _cmsgs.headerSignup(), Link.createListener(Pages.ACCOUNT, "create"));
            signup.setStyleName("SignupButton");
            signup.addStyleName("Button");
            setWidget(0, 0, signup);
            getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
            setWidget(0, 1, WidgetUtil.makeShim(10, 10));
            PushButton logon = new PushButton(_cmsgs.headerLogon(), new ClickListener() {
                public void onClick (Widget sender) {
                    setWidget(0, 2, new LogonPanel(true));
                }
            });
            logon.setStyleName("LogonButton");
            logon.addStyleName("Button");
            setWidget(0, 2, logon);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
    }

    protected int _statusCol;
    protected List<NaviButton> _buttons = new ArrayList<NaviButton>();
    protected StatusPanel _status = new StatusPanel();

    protected static final NaviImages _images = (NaviImages)GWT.create(NaviImages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
