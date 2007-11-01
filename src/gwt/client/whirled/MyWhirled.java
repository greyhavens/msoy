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

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;

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
import client.util.MsoyUI;

public class MyWhirled extends FlexTable
{
    public MyWhirled (final PopulationDisplay popDisplay)
    {
        buildUI();

        CWhirled.membersvc.getMyWhirled(CWhirled.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                MyWhirledData data = (MyWhirledData) result;
                popDisplay.displayPopulation(data.whirledPopulation);
                fillUI(data);
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CWhirled.serverError(caught));
            }
        });
    }

    protected void buildUI ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(0);

        getFlexCellFormatter().setRowSpan(row, 0, 3);
        getFlexCellFormatter().setStyleName(row, 0, "MePanelContainer");
        VerticalPanel mePanel = new VerticalPanel();
        mePanel.setStyleName("MePanel");
        setWidget(row, 0, mePanel);
        VerticalPanel descriptionPanel = new VerticalPanel();
        descriptionPanel.setStyleName("Description");
        descriptionPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        descriptionPanel.add(new HTML(CWhirled.msgs.myWhirledDescription()));
        Hyperlink whirledwideLink =
            Application.createLink(CWhirled.msgs.titleWhirledwide(), Page.WHIRLED, "whirledwide");
        whirledwideLink.setStyleName("Whirledwide");
        descriptionPanel.add(whirledwideLink);
        descriptionPanel.add(new HTML(CWhirled.msgs.whirledwideDescription()));
        mePanel.add(descriptionPanel);
        mePanel.add(_pictureBox = new VerticalPanel());
        _pictureBox.setStyleName("PictureBox");
        _pictureBox.addStyleName("borderedBox");
        _pictureBox.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _pictureBox.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        HorizontalPanel header = new HorizontalPanel();
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.setStyleName("SectionHeaderContainer");
        Label title = new Label(CWhirled.msgs.headerRooms());
        title.setStyleName("SectionHeader");
        header.add(title);
        mePanel.add(header);
        mePanel.add(_roomsBox = new VerticalPanel());
        _roomsBox.setStyleName("RoomsBox");
        _roomsBox.addStyleName("borderedBox");
        _roomsBox.setSpacing(3);
        header = new HorizontalPanel();
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.setStyleName("SectionHeaderContainer");
        title = new Label(CWhirled.msgs.headerChats());
        title.setStyleName("SectionHeader");
        header.add(title);
        mePanel.add(header);
        mePanel.add(_chatsBox = new VerticalPanel());
        _chatsBox.setStyleName("ChatsBox");
        _chatsBox.addStyleName("borderedBox");
        _chatsBox.setSpacing(3);

        _peopleAttributes = new HashMap();

        getFlexCellFormatter().setColSpan(row, 1, 2);
        setWidget(row++, 1, _people = new PagedGrid(PEOPLE_ROWS, PEOPLE_COLUMNS) {
            protected Widget createWidget (Object item) {
                if (item == null) {
                    Label shim = new Label();
                    shim.setStyleName("PersonWidget");
                    return shim;
                } else {
                    return new PersonWidget((MemberCard) item, _peopleAttributes);
                }
            }
            protected String getEmptyMessage () {
                return CWhirled.msgs.noPeople();
            }
            protected String getHeaderText (int start, int limit, int total) {
                return CWhirled.msgs.headerPeople();
            }
            protected boolean alwaysDisplayNavi () {
                return true;
            }
        });
        _people.addStyleName("PeopleContainer");

        VerticalPanel placesContainer = new VerticalPanel();
        setWidget(row, 0, placesContainer);
        placesContainer.setStyleName("PlacesContainer");
        header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        Label star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        title = new Label(CWhirled.msgs.headerPlaces());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        placesContainer.add(header);
        placesContainer.add(_places = new SceneList(SceneCard.ROOM));

        VerticalPanel gamesContainer = new VerticalPanel();
        setWidget(row++, 1, gamesContainer);
        gamesContainer.setStyleName("GamesContainer");
        header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        title = new Label(CWhirled.msgs.headerGames());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        gamesContainer.add(header);
        gamesContainer.add(_games = new SceneList(SceneCard.GAME));

        getFlexCellFormatter().setColSpan(row, 0, 2);
        setWidget(row++, 0, _feed = new FeedPanel());
    }

    protected void fillUI (MyWhirledData myWhirled)
    {
        List people = myWhirled.people;
        Object[] peopleArray = people.toArray();
        // sort alphabetically
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

        // add our own profile picture to the left column
        MediaDesc photo = (myWhirled.photo == null) ? Profile.DEFAULT_PHOTO : myWhirled.photo;
        Widget image = MediaUtil.createMediaView(photo, MediaDesc.THUMBNAIL_SIZE);
        if (image instanceof Image) {
            ((Image)image).addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "" + CWhirled.getMemberId());
                }
            });
        }
        _pictureBox.add(image);

        // show the player's rooms in purchased order by doing an ascending sort on the sceneIds
        Object[] sceneIds = myWhirled.ownedRooms.keySet().toArray();
        Arrays.sort(sceneIds);
        // TODO: if a user has too many rooms, we need to scroll the _roomsBox vertically, instead
        // of letting it grow indefinitely
        for (int ii = 0; ii < sceneIds.length; ii++) {
            _roomsBox.add(Application.createLink((String)(myWhirled.ownedRooms.get(sceneIds[ii])),
                                                 Page.WORLD, "s" + sceneIds[ii]));
        }

        if (myWhirled.chats == null || myWhirled.chats.size() == 0) {
            _chatsBox.add(new Label(CWhirled.msgs.noChats()));

        } else {
            // show group chats in group-created order by sorting on group ids
            Object[] chatIds = myWhirled.chats.keySet().toArray();
            Arrays.sort(chatIds);
            for (int ii = 0; ii < chatIds.length; ii++) {
                _chatsBox.add(Application.createLink((String)(myWhirled.chats.get(chatIds[ii])),
                                                     Page.WORLD, "c" + chatIds[ii]));
            }
        }

        // configure the feed panel
        _feed.setFeed(myWhirled.feed, false);
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
                logo = MediaUtil.createMediaView(scene.logo, MediaDesc.THUMBNAIL_SIZE);
            } else if (scene.sceneType == SceneCard.GAME) {
                MediaDesc gameLogo = Item.getDefaultThumbnailMediaFor(Item.GAME);
                logo = MediaUtil.createMediaView(gameLogo, MediaDesc.THUMBNAIL_SIZE);
            }
            if (logo instanceof Image) {
                ((Image) logo).addClickListener(goToScene);
            }
            logoContainer.add(logo);
            add(logoContainer);

            VerticalPanel text = new VerticalPanel();
            text.setStyleName("Text");
            Label nameLabel = new Label("" + scene.name);
            nameLabel.setStyleName("SceneName");
            nameLabel.addClickListener(goToScene);
            text.add(nameLabel);
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
                    String link = Application.createLinkHtml(
                        CWhirled.msgs.viewProfile(), "profile", "" + id);
                    menu.addItem(link, true, new Command() {
                        public void execute () {
                            personMenuPanel.hide();
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
                            Application.go(Page.PROFILE, "" + id);
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

            Widget pic = MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE);
            if (pic instanceof Image && goToFriend != null) {
                ((Image) pic).addClickListener(goToFriend);
            }
            add(pic);
            Label nameLabel = new Label("" + card.name);
            if (goToFriend != null) {
                nameLabel.addClickListener(goToFriend);
            }
            nameLabel.setStyleName("NameLabel");
            if (attrs != null && attrs.size() >= 2) {
                nameLabel.addStyleName("" + attrs.get(1));
            }
            add(nameLabel);
        }
    }

    protected static final int PEOPLE_ROWS = 4;
    protected static final int PEOPLE_COLUMNS = 6;

    protected PagedGrid _people;
    protected SceneList _places;
    protected SceneList _games;
    protected FeedPanel _feed;

    protected VerticalPanel _pictureBox;
    protected VerticalPanel _roomsBox;
    protected VerticalPanel _chatsBox;

    /** Map of member Ids to a List of attributes for the person.
     * List is: [ name, style for name label ] */
    protected Map _peopleAttributes;

    /** List of the people who are sitting at a pending table. */
    protected List _pendingTableMembers = new ArrayList();
}
