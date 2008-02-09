//
// $Id$

package client.whirled;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;
import com.threerings.msoy.web.data.SceneCard;

import com.threerings.msoy.data.all.MemberName;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.FlashClients;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class MyWhirled extends FlexTable
{
    public MyWhirled (final PopulationDisplay popDisplay)
    {
        setStyleName("myWhirled");
        buildUI();

        CWhirled.worldsvc.getMyWhirled(CWhirled.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                MyWhirledData data = (MyWhirledData) result;
                popDisplay.displayPopulation(data.whirledPopulation);
                fillUI(data);
            }
        });
    }

    protected void buildUI ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(0);

        getFlexCellFormatter().setRowSpan(row, 0, 3);
        getFlexCellFormatter().setStyleName(row, 0, "mePanelContainer");

        _peopleAttributes = new HashMap();
        getFlexCellFormatter().setColSpan(row, 1, 2);
        _people = new PagedGrid(PEOPLE_ROWS, PEOPLE_COLUMNS, PagedGrid.NAV_ON_BOTTOM) {
            protected Widget createWidget (Object item) {
                if (item == null) {
                    return MsoyUI.createLabel("", "PersonWidget");
                } else {
                    return new PersonWidget((MemberCard) item, _peopleAttributes);
                }
            }
            protected String getEmptyMessage () {
                return CWhirled.msgs.noPeople();
            }
            protected boolean displayNavi (int items) {
                return true;
            }
            protected void addCustomControls (FlexTable controls) {
                Hyperlink link = Application.createLink(
                    "All your friends...", Page.PROFILE, Args.compose("f", CWhirled.getMemberId()));
                link.addStyleName("nowrapLabel");
                controls.setWidget(0, 0, link);
            }
        };
        _people.addStyleName("dottedGrid");
        _people.setWidth("100%");
        setWidget(row++, 1, MsoyUI.createBox("people", CWhirled.msgs.headerPeople(), _people));
        setWidget(row, 0, MsoyUI.createBox("places", CWhirled.msgs.headerPlaces(),
                                           _places = new SceneList(SceneCard.ROOM)));
        setWidget(row++, 1, MsoyUI.createBox("games", CWhirled.msgs.headerGames(),
                                             _games = new SceneList(SceneCard.GAME)));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, _feed = new FeedPanel());
    }

    protected Widget createHeader (String title)
    {
        HorizontalPanel header = new HorizontalPanel();
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.setStyleName("SectionHeaderContainer");
        header.add(MsoyUI.createLabel(title, "SectionHeader"));
        return header;
    }

    protected VerticalPanel createListBox (String styleName)
    {
        VerticalPanel box = new VerticalPanel();
        box.setStyleName(styleName);
        box.addStyleName("borderedBox");
        box.setSpacing(3);
        return box;
    }

    protected void fillUI (MyWhirledData myWhirled)
    {
        // set up our sidebar
        VerticalPanel sidebar = new VerticalPanel(), box;
        sidebar.setStyleName("mePanel");
        setWidget(0, 0, sidebar);

        // add our own profile picture to the left column
        sidebar.add(createHeader(CWhirled.msgs.headerProfile()));
        sidebar.add(box = createListBox("PictureBox"));
        box.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        MediaDesc photo = (myWhirled.photo == null) ? Profile.DEFAULT_PHOTO : myWhirled.photo;
        box.add(MediaUtil.createMediaView(photo, MediaDesc.THUMBNAIL_SIZE, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.PROFILE, "" + CWhirled.getMemberId());
            }
        }));

        // add a list of tools
        sidebar.add(createHeader(CWhirled.msgs.headerTools()));
        sidebar.add(box = createListBox("ListBox"));
        box.add(Application.createLink("My Discussions", Page.GROUP, "unread"));
        box.add(Application.createLink("My Mail", Page.MAIL, ""));
        box.add(Application.createLink("My Account", Page.ACCOUNT, "edit"));
        if (CWhirled.isSupport()) {
            box.add(Application.createLink("Admin Console", Page.ADMIN, ""));
        }
        box.add(MsoyUI.createActionLabel("Logoff", new ClickListener() {
            public void onClick (Widget sender) {
                CWhirled.app.didLogoff();
            }
        }));

        // add all of our rooms
        sidebar.add(createHeader(CWhirled.msgs.headerRooms()));
        sidebar.add(box = createListBox("ListBox"));

        // first add our home room
        Integer homeId = new Integer(myWhirled.homeSceneId);
        box.add(createIconLink("/images/whirled/my_home.png",
                               (String)myWhirled.ownedRooms.get(homeId),
                               Page.WORLD, "s" + homeId));

        // next add the remainder of our rooms in purchased order (lowest scene id first)
        Object[] sceneIds = myWhirled.ownedRooms.keySet().toArray();
        Arrays.sort(sceneIds);
        for (int ii = 0; ii < sceneIds.length; ii++) {
            if (homeId.equals(sceneIds[ii])) {
                continue;
            }
            String sname = (String)myWhirled.ownedRooms.get(sceneIds[ii]);
            box.add(Application.createLink(sname, Page.WORLD, "s" + sceneIds[ii]));
        }

        // add links to our stuff
        sidebar.add(createHeader(CWhirled.msgs.headerStuff()));
        sidebar.add(box = createListBox("ListBox"));
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            byte type = Item.TYPES[ii];
            box.add(createIconLink(Item.getDefaultThumbnailMediaFor(type).getMediaPath(),
                                   CWhirled.dmsgs.getString("pItemType" + type),
                                   Page.INVENTORY, "" + type));
        }

        // add our active chats, if we have any
        if (myWhirled.chats != null && myWhirled.chats.size() > 0) {
            sidebar.add(createHeader(CWhirled.msgs.headerChats()));
            sidebar.add(box = createListBox("ListBox"));
            // show group chats in group-created order by sorting on group ids
            Object[] chatIds = myWhirled.chats.keySet().toArray();
            Arrays.sort(chatIds);
            for (int ii = 0; ii < chatIds.length; ii++) {
                String cname = (String)myWhirled.chats.get(chatIds[ii]);
                box.add(Application.createLink(cname, Page.WORLD, "c" + chatIds[ii]));
            }
        }

        // sort our friends list alphabetically and map them by id
        List people = myWhirled.people;
        Object[] peopleArray = people.toArray();
        Arrays.sort(peopleArray, new Comparator() {
            public int compare (Object o1, Object o2) {
                if (!(o1 instanceof MemberCard) || !(o2 instanceof MemberCard)) {
                    return 0;
                }
                MemberCard m1 = (MemberCard) o1;
                MemberCard m2 = (MemberCard) o2;
                return ("" + m1.name).compareTo("" + m2.name);
            }
            public boolean equals (Object obj) {
                return obj == this;
            }
        });
        for (int ii = 0; ii < peopleArray.length; ii++) {
            MemberName person = ((MemberCard) peopleArray[ii]).name;
            ArrayList list = new ArrayList();
            list.add("" + person);
            _peopleAttributes.put(new Integer(person.getMemberId()), list);
        }
        people = Arrays.asList(peopleArray);

        // populate _peopleAttributes with scene type info
        List[] scenes = { myWhirled.places, myWhirled.games };
        for (int ii = 0; ii < scenes.length; ii++) {
            Iterator sceneIter = scenes[ii].iterator();
            while (sceneIter.hasNext()) {
                final SceneCard card = (SceneCard) sceneIter.next();
                Iterator friendIter = card.friends.iterator();
                while (friendIter.hasNext()) {
                    final Object id = friendIter.next();
                    List entry = (List) _peopleAttributes.get(id);
                    if (entry != null) {
                        if (entry.size() == 1) {
                            // make sure there is already something at index 1 so that entry.set()
                            // is happy
                            entry.add(null);
                        } else {
                            // if the list had an entry at index 1, then this person is in both
                            // a game and a scene, meaning that they're at a pending table.
                            _pendingTableMembers.add(id);
                        }
                        entry.set(1, card.sceneType == SceneCard.ROOM ? "Room" : "Game");
                    }
                }
            }
        }

        // ensure that the list has either >= PEOPLE_COLUMNS or 0 entries for spacing reasons
        while (people.size() != 0 && people.size() < PEOPLE_COLUMNS) {
            people.add(null);
        }
        _people.setModel(new SimpleDataModel(people), 0);
        _places.populate(myWhirled.places, _peopleAttributes);
        _games.populate(myWhirled.games, _peopleAttributes, _pendingTableMembers);

        // configure the feed panel
        _feed.setFeed(myWhirled.feed, false);
    }

    protected Widget createIconLink (String icon, String label, final String page, final String args)
    {
        HorizontalPanel row = new HorizontalPanel();
        Image image = MsoyUI.createActionImage(icon, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(page, args);
            }
        });
        image.setWidth("18px");
        row.add(image);
        row.add(WidgetUtil.makeShim(2, 2));
        row.add(Application.createLink(label, page, args));
        return row;
    }

    protected static class SceneList extends ScrollPanel
    {
        public SceneList (int sceneType)
        {
            _sceneType = sceneType;
            setStyleName("SceneList");
            setAlwaysShowScrollBars(true);
            // why the hell doesn't GWT support only scrolling in one direction?
            DOM.setStyleAttribute(getElement(), "overflowX", "hidden");
        }

        public void populate (List scenes, Map peopleAttributes)
        {
            populate(scenes, peopleAttributes, null);
        }

        public void populate (List scenes, Map peopleAttributes, List pendingTableMembers)
        {
            VerticalPanel sceneContainer = new VerticalPanel();
            sceneContainer.setStyleName("SceneListContainer");
            sceneContainer.setSpacing(0);
            setWidget(sceneContainer);
            if (scenes.size() == 0) {
                showEmptyEntry(sceneContainer);
                return;
            }

            // sort by number of friends in the room, then total population, desc
            Object[] sceneArray = scenes.toArray();
            Arrays.sort(sceneArray, new Comparator() {
                public int compare (Object o1, Object o2) {
                    if (!(o1 instanceof SceneCard) || !(o2 instanceof SceneCard)) {
                        return 0;
                    }

                    SceneCard s1 = (SceneCard) o1;
                    SceneCard s2 = (SceneCard) o2;
                    if (s2.friends.size() == s1.friends.size()) {
                        return s2.population - s1.population;
                    } else {
                        return s2.friends.size() - s1.friends.size();
                    }
                }
                public boolean equals (Object obj) {
                    return obj == this;
                }
            });

            for (int ii = 0; ii < sceneArray.length; ii++) {
                SceneCard scene = (SceneCard) sceneArray[ii];
                sceneContainer.add(new SceneWidget(scene, peopleAttributes, pendingTableMembers));
            }
        }

        /**
         * adds the empty list entry to this SceneList so that the list is never completely empty,
         * which is so sad.
         */
        protected void showEmptyEntry (VerticalPanel sceneContainer)
        {
            HorizontalPanel fakeSceneWidget = new HorizontalPanel();
            fakeSceneWidget.setStyleName("SceneWidget");
            fakeSceneWidget.addStyleName("Fake");
            fakeSceneWidget.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            fakeSceneWidget.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            sceneContainer.add(fakeSceneWidget);

            if (_sceneType == SceneCard.ROOM) {
                fakeSceneWidget.add(new HTML(CWhirled.msgs.emptyPopularPlaces(
                    Application.createLinkToken("world", "m" + CWhirled.creds.getMemberId()),
                    Application.createLinkToken(Page.WHIRLED, "whirledwide"))));
            } else if (_sceneType == SceneCard.GAME) {
                fakeSceneWidget.add(new HTML(CWhirled.msgs.emptyActiveGames(
                    Application.createLinkToken(Page.WHIRLED, "whirledwide"))));
            }
        }

        protected int _sceneType;
    }

    protected static class SceneWidget extends HorizontalPanel
    {
        public SceneWidget (final SceneCard scene, Map peopleAttributes, List pendingTableMembers)
        {
            setStyleName("SceneWidget");
            setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

            ClickListener goToScene = new ClickListener() {
                public void onClick (Widget sender) {
                    if (scene.sceneType == SceneCard.ROOM) {
                        Application.go(Page.WORLD, "s" + scene.sceneId);
                    } else {
                        Application.go(Page.GAME, Args.compose("d", scene.sceneId /* gameId */));
                    }
                }
            };

            HorizontalPanel logoContainer = new HorizontalPanel();
            logoContainer.setStyleName("LogoContainer");
            logoContainer.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            logoContainer.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            Widget logo = null;
            if (scene.logo != null) {
                logo = MediaUtil.createMediaView(scene.logo, MediaDesc.THUMBNAIL_SIZE, goToScene);
            } else if (scene.sceneType == SceneCard.GAME) {
                MediaDesc gameLogo = Item.getDefaultThumbnailMediaFor(Item.GAME);
                logo = MediaUtil.createMediaView(gameLogo, MediaDesc.THUMBNAIL_SIZE, goToScene);
            }
            logoContainer.add(logo);
            add(logoContainer);

            VerticalPanel text = new VerticalPanel();
            text.setStyleName("Text");
            text.add(MsoyUI.createActionLabel(""+scene.name, "SceneName", goToScene));
            Iterator peopleIter = scene.friends.iterator();
            FlowPanel peopleList = new FlowPanel();
            if (peopleIter.hasNext()) {
                InlineLabel population = new InlineLabel(
                    CWhirled.msgs.populationFriends(CWhirled.msgs.population(
                        "" + Math.max(scene.population, scene.friends.size()))));
                population.addStyleName("PopulationLabel");
                peopleList.add(population);
            } else {
                InlineLabel population = new InlineLabel(CWhirled.msgs.population(
                    "" + scene.population));
                peopleList.add(population);
            }
            String visiblePeopleList = "";
            while (peopleIter.hasNext()) {
                final Object id = peopleIter.next();
                List attrs = (List) peopleAttributes.get(id);
                if (attrs == null) {
                    continue;
                }

                visiblePeopleList += "" + attrs.get(0);
                if (visiblePeopleList.length() > 50) {
                    peopleList.remove(peopleList.getWidgetCount() - 1);
                    InlineLabel ellipses = new InlineLabel("...");
                    ellipses.setStyleName("GrayName");
                    peopleList.add(ellipses);
                    break;
                }

                final InlineLabel person = new InlineLabel("" + attrs.get(0));
                person.addStyleName("Underline");
                person.addStyleName("GrayName");
                if (scene.sceneType == SceneCard.GAME) {
                    final PopupPanel personMenuPanel = new PopupPanel(true);
                    MenuBar menu = new MenuBar(true);
                    menu.addItem(CWhirled.msgs.viewProfile(), new Command() {
                        public void execute () {
                            Application.go(Page.PROFILE, ""+id);
                        }
                    });
                    boolean inPending = (pendingTableMembers != null) &&
                        pendingTableMembers.contains(id);
                    final String flashArg = (inPending ? "playerTable=" : "memberScene=") + id;
                    menu.addItem(CWhirled.msgs.goToGame("" + attrs.get(0)), new Command() {
                        public void execute () {
                            WorldClient.displayFlash(flashArg);
                            personMenuPanel.hide();
                        }
                    });
                    personMenuPanel.add(menu);
                    person.addMouseListener(new MouseListenerAdapter() {
                        public void onMouseDown (Widget sender, int x, int y) {
                            personMenuPanel.setPopupPosition(person.getAbsoluteLeft() + x,
                                person.getAbsoluteTop() + y);
                            personMenuPanel.show();
                        }
                    });
                } else {
                    person.addClickListener(new ClickListener() {
                        public void onClick (Widget sender) {
                            Application.go(Page.PROFILE, ""+id);
                        }
                    });
                }
                peopleList.add(person);
                InlineLabel connector = new InlineLabel(peopleIter.hasNext() ? ", " : ".");
                connector.addStyleName("GrayName");
                peopleList.add(connector);
            }
            text.add(peopleList);
            add(text);
        }
    }

    protected static class PersonWidget extends VerticalPanel
    {
        public PersonWidget (final MemberCard card, Map peopleAttributes)
        {
            setStyleName("PersonWidget");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

            List attrs = (List) peopleAttributes.get(new Integer(card.name.getMemberId()));
            ClickListener goToFriend = new ClickListener() {
                public void onClick (Widget sender) {
                    WorldClient.displayFlash("memberScene=" + card.name.getMemberId());
                }
            };

            add(MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE, goToFriend));
            Widget nameLabel = MsoyUI.createActionLabel("" + card.name, "NameLabel", goToFriend);
            if (attrs != null && attrs.size() >= 2) {
                nameLabel.addStyleName("" + attrs.get(1));
            }
            add(nameLabel);
        }
    }

    protected static final int PEOPLE_ROWS = 2;
    protected static final int PEOPLE_COLUMNS = 6;

    protected PagedGrid _people;
    protected SceneList _places;
    protected SceneList _games;
    protected FeedPanel _feed;

    /** Map of member Ids to a List of attributes for the person.
     * List is: [ name, style for name label ] */
    protected Map _peopleAttributes;

    /** List of the people who are sitting at a pending table. */
    protected List _pendingTableMembers = new ArrayList();
}
