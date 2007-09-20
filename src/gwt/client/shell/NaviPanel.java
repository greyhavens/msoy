//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.MemberInvites;
import com.threerings.msoy.web.data.WebCreds;

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
        // replace our logon menu item with the "me" menu item
        int menuidx = 0;
        setMenu(menuidx++, "Me", CShell.cmsgs.menuMe(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "My Whirled", "whirled", "mywhirled");
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
                addLink(menu, "My Whirled", "whirled", "mywhirled");
                addLink(menu, "My Home", "world", "m" + creds.getMemberId());
                if (_scenes.size() > 0) {
                    MenuBar smenu = new MenuBar(true);
                    Iterator siter = _scenes.iterator();
                    while (siter.hasNext()) {
                        SceneData data = (SceneData)siter.next();
                        addLink(smenu, data.name, "world", "s" + data.id);
                    }
                    menu.addItem("My Rooms", smenu);
                }
                if (_friends.size() > 0) {
                    MenuBar fmenu = new MenuBar(true);
                    for (int ii = 0, ll = _friends.size(); ii < ll; ii++) {
                        MemberName name = (MemberName)_friends.get(ii);
                        addLink(fmenu, name + "'s Home", "world", "m" + name.getMemberId());
                    }
                    menu.addItem("Friends' Homes", fmenu);
                }
                if (CShell.isSupport()) {
                    addLink(menu, "Admin Console", "admin", "");
                }
                addLink(menu, "Projects", "swiftly", "");
            }
        });

        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                MenuBar fmenu = new MenuBar(true);
                fmenu.addItem("Find People", true, new Command() {
                    public void execute () {
                        History.newItem(Application.createLinkToken("profile", ""));
                        _popped.hide();
                    }
                });
                for (int ii = 0, ll = _friends.size(); ii < ll; ii++) {
                    MemberName name = (MemberName)_friends.get(ii);
                    addLink(fmenu, name.toString(), "profile", "" + name.getMemberId());
                }
                menu.addItem("Profiles", fmenu);
                addLink(menu, "Groups", "group", "");
                addLink(menu, "Forums", "wrap", "f");
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
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type),
                            "inventory", "" + type);
                }
            }
        });

        setMenu(menuidx++, "Catalog", CShell.cmsgs.menuCatalog(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type), "catalog", "" + type);
                }
            }
        });

        setMenu(menuidx++, "Help", CShell.cmsgs.menuHelp(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "About Whirled", "wrap", "w");
                addLink(menu, "Tutorials", "wrap", "w-Category:Tutorials");
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
                addLink(menu, "Whirledwide", "whirled", "whirledwide");
            }
        });

        setMenu(menuidx++, "People", CShell.cmsgs.menuPeople(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "Groups", "group", "");
            }
        });

        setMenu(menuidx++, "Catalog", CShell.cmsgs.menuCatalog(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                for (int ii = 0; ii < Item.TYPES.length; ii++) {
                    byte type = Item.TYPES[ii];
                    addLink(menu, CShell.dmsgs.getString("pItemType" + type), "catalog", "" + type);
                }
            }
        });

        setMenu(menuidx++, "Help", CShell.cmsgs.menuHelp(), new MenuPopper() {
            protected void populateMenu (Widget sender, MenuBar menu) {
                addLink(menu, "About Whirled", "wrap", "w");
                addLink(menu, "Tutorials", "wrap", "w-Category:Tutorials");
            }
        });

        setText(0, menuidx++, ""); // clear the last menu
        _friends.clear();
        _scenes.clear();
    }

    protected void setMenu (int menuidx, final String ident, String text, ClickListener listener)
    {
        final VerticalPanel box = new VerticalPanel();
        box.setStyleName(ident);
        MouseListenerAdapter fiddle = new MouseListenerAdapter() {
            public void onMouseEnter (Widget sender) {
                box.setStyleName(ident + "Hover");
            }
            public void onMouseLeave (Widget sender) {
                box.setStyleName(ident);
            }
        };
        Label lbl;
        box.add(lbl = MsoyUI.createCustomActionLabel("", "Button", listener));
        lbl.addMouseListener(fiddle);
        box.add(lbl = MsoyUI.createCustomActionLabel(text, "Link", listener));
        lbl.addMouseListener(fiddle);
        setWidget(0, menuidx, box);
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

    protected class SceneData {
        String name;
        int id;

        public SceneData (String name, int id)
        {
            this.name = name;
            this.id = id;
        }
    };
    
    protected StatusPanel _status;
    protected Label _loglbl, _melbl;

    /** The currently popped up menu, for easy closing. */
    protected PopupPanel _popped;

    /** Our friends. */
    protected ArrayList _friends = new ArrayList();

    /** Owned scenes. */
    protected ArrayList _scenes = new ArrayList(); // of SceneData
}
