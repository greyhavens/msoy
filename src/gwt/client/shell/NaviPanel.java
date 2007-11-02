//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.images.NaviImages;

import client.util.MsoyUI;
import client.util.events.FlashEvents;
import client.util.events.FriendEvent;
import client.util.events.FriendsListener;
import client.util.events.SceneBookmarkEvent;
import client.util.events.SceneBookmarkListener;

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

        // register to hear about data additions and removals
        FlashEvents.addListener(new FriendsListener() {
            public void friendAdded (FriendEvent event) {
                _friends.add(event.getFriend());
                Collections.sort(_friends, new Comparator() {
                    public int compare (Object o1, Object o2) {
                        return MemberName.compareNames((MemberName)o1, (MemberName)o2);
                    }
                });
            }
            public void friendRemoved (FriendEvent event) {
                _friends.remove(event.getFriend());
            }
        });
        FlashEvents.addListener(new SceneBookmarkListener() {
            public void sceneAdded (SceneBookmarkEvent event) {
                _scenes.add(new SceneData(event.getSceneName(), event.getSceneId()));
            }
            public void sceneRemoved (SceneBookmarkEvent event) {
                Iterator iter = _scenes.iterator();
                while (iter.hasNext()) {
                    SceneData candidate = (SceneData)iter.next();
                    if (candidate.id == event.getSceneId()) {
                        iter.remove();
                        break;
                    }
                }
            }
        });
    }

    /**
     * Called when the player logs on (or navigates to a page and is already logged on).
     */
    public void didLogon (final WebCreds creds)
    {
        int menuidx = 0;
        ClickListener click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Whirled", Page.WHIRLED, "mywhirled");
                addLink(menu, "My Home", Page.WORLD, "m" + creds.getMemberId());
                addLink(menu, "My Profile", Page.PROFILE, "" + creds.getMemberId());
                addLink(menu, "My Mail", Page.MAIL, "");
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
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuMe(), _images.me(), _images.ome(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Whirled", Page.WHIRLED, "mywhirled");
                addLink(menu, "Whirledwide", Page.WHIRLED, "whirledwide");
                addLink(menu, "My Home", Page.WORLD, "m" + creds.getMemberId());
                if (_scenes.size() > 0) {
                    MenuBar smenu = new MenuBar(true);
                    Iterator siter = _scenes.iterator();
                    while (siter.hasNext()) {
                        SceneData data = (SceneData)siter.next();
                        addLink(smenu, data.name, Page.WORLD, "s" + data.id);
                    }
                    menu.addItem("My Rooms", smenu);
                }
                if (_friends.size() > 0) {
                    MenuBar fmenu = new MenuBar(true);
                    createMenu(fmenu, _friends, new ItemCreator() {
                        public void createItem (MenuBar menu, Object item) {
                            MemberName name = (MemberName)item;
                            addLink(menu, name + "'s Home", Page.WORLD, "m" + name.getMemberId());
                        }
                    });
                    menu.addItem("Friends' Homes", fmenu);
                }
                if (CShell.isSupport()) {
                    addLink(menu, "Admin Console", Page.ADMIN, "");
                }
                addLink(menu, "Projects", Page.SWIFTLY, "");
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuPlaces(), _images.places(), _images.oplaces(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                MenuBar fmenu = new MenuBar(true);
                fmenu.addItem("Find People", true, new Command() {
                    public void execute () {
                        Application.go(Page.PROFILE, "search");
                        _popped.hide();
                    }
                });
                createMenu(fmenu, _friends, new ItemCreator() {
                    public void createItem (MenuBar menu, Object item) {
                        MemberName name = (MemberName)item;
                        addLink(menu, name.toString(), Page.PROFILE, "" + name.getMemberId());
                    }
                });
                menu.addItem("Profiles", fmenu);
                addLink(menu, "Groups", Page.GROUP, "");
                addLink(menu, "Forums", Page.WRAP, "f");
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
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuPeople(), _images.people(), _images.opeople(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type),
                            Page.INVENTORY, "" + type);
                }
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuStuff(), _images.stuff(), _images.ostuff(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type),
                            Page.CATALOG, "" + type);
                }
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuCatalog(), _images.catalog(), _images.ocatalog(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "About Whirled", Page.WRAP, "w");
                addLink(menu, "Tutorials", Page.WRAP, Args.compose("w", "Category:Tutorials"));
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuHelp(), _images.help(), _images.ohelp(), click));
    }

    protected void createMenu (MenuBar menu, ArrayList items, ItemCreator creator)
    {
        // if the menu is not too long, just put everything in directly
        if (items.size() <= MENU_OVERFLOW) {
            for (int ii = 0, ll = items.size(); ii < ll; ii++) {
                creator.createItem(menu, items.get(ii));
            }
            return;
        }

        // otherwise switch to breakout sub-menus
        int start = 0;
        while (start < items.size()) {
            int end = Math.min(items.size(), start + MENU_OVERFLOW);
            String title = (items.get(start).toString().substring(0, 1) + " - " +
                            items.get(end-1).toString().substring(0, 1));
            MenuBar smenu = new MenuBar(true);
            menu.addItem(title, smenu);
            for (int ii = start; ii < end; ii++) {
                creator.createItem(smenu, items.get(ii));
            }
            start = (end+1);
        }
    }

    /**
     * Called when the player logs off (or navigates to a page and is currently logged off).
     */
    public void didLogoff ()
    {
        int menuidx = 0;
        ClickListener click = new ClickListener() {
            public void onClick (Widget sender) {
                LogonPanel.toggleShowLogon(_status);
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuLogon(), _images.me(), _images.ome(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Whirledwide", Page.WHIRLED, "whirledwide");
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuPlaces(), _images.places(), _images.oplaces(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Groups", Page.GROUP, "");
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuPeople(), _images.people(), _images.opeople(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type),
                            Page.CATALOG, "" + type);
                }
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuCatalog(), _images.catalog(), _images.ocatalog(), click));

        click = new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "About Whirled", Page.WRAP, "w");
                addLink(menu, "Tutorials", Page.WRAP, Args.compose("w", "Category:Tutorials"));
            }
        };
        setWidget(0, menuidx++, new NaviButton(
                      CShell.cmsgs.menuHelp(), _images.help(), _images.ohelp(), click));

        setText(0, menuidx++, ""); // clear the last menu
        _friends.clear();
        _scenes.clear();
    }

    protected void addLink (MenuBar menu, String text, final String page, final String args)
    {
        menu.addItem(text, false, new Command() {
            public void execute () {
                Application.go(page, args);
                _popped.hide();
            }
        });
    }

    protected abstract class MenuPopper implements ClickListener
    {
        public void onClick (Widget sender)
        {
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
            Widget box = from.getParent();
            return box.getAbsoluteTop() + box.getOffsetHeight();
        }

        protected abstract void populateMenu (Widget sender, MenuBar menu);
    }

    protected static class NaviButton extends FlowPanel
    {
        public NaviButton (String text, AbstractImagePrototype upImage,
                           AbstractImagePrototype overImage, ClickListener listener)
        {
            MouseListenerAdapter fiddle = new MouseListenerAdapter() {
                public void onMouseEnter (Widget sender) {
                    clear();
                    add(_overImage);
                    add(_label);
                }
                public void onMouseLeave (Widget sender) {
                    clear();
                    add(_upImage);
                    add(_label);
                }
            };

            _upImage = upImage.createImage();
            _upImage.setStyleName("actionLabel");
            _upImage.addMouseListener(fiddle);
            _overImage = overImage.createImage();
            _overImage.setStyleName("actionLabel");
            _overImage.addMouseListener(fiddle);
            _overImage.addClickListener(listener);

            _label = MsoyUI.createCustomActionLabel(text, "Link", listener);
            _label.addMouseListener(fiddle);

            add(_upImage);
            add(_label);
        }

        protected Image _upImage, _overImage;
        protected Label _label;
    }

    protected static class SceneData
    {
        public String name;
        public int id;

        public SceneData (String name, int id)
        {
            this.name = name;
            this.id = id;
        }
    };

    protected interface ItemCreator
    {
        public void createItem (MenuBar menu, Object item);
    }
    
    protected StatusPanel _status;
    protected Label _loglbl, _melbl;

    /** Our navigation menu images. */
    protected NaviImages _images = (NaviImages)GWT.create(NaviImages.class);

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;

    /** Our friends. */
    protected ArrayList _friends = new ArrayList();

    /** Owned scenes. */
    protected ArrayList _scenes = new ArrayList(); // of SceneData

    protected static final int MENU_OVERFLOW = 20;
}
