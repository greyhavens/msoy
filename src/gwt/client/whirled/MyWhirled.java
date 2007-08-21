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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.Whirled;

import com.threerings.msoy.data.all.MemberName;

import client.util.FlashClients;
import client.util.MediaUtil;

import client.shell.Application;
import client.shell.WorldClient;

public class MyWhirled extends FlexTable
{
    public MyWhirled ()
    {
        buildUi();

        CWhirled.membersvc.getMyWhirled(CWhirled.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                fillUi((Whirled) result);
            }
            public void onFailure (Throwable caught) {
                _errorContainer.add(new Label(CWhirled.serverError(caught)));
            }
        });
    }

    protected void buildUi ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(0);

        getFlexCellFormatter().setRowSpan(row, 0, 3);
        getFlexCellFormatter().setStyleName(row, 0, "MePanelContainer");
        VerticalPanel mePanel = new VerticalPanel();
        mePanel.setStyleName("MePanel");
        setWidget(row, 0, mePanel);
        HTML description = new HTML(CWhirled.msgs.myWhirledDescription());
        description.setStyleName("Description");
        mePanel.add(description);
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
        
        setWidget(row++, 1, _errorContainer = new HorizontalPanel());

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
            protected boolean alwaysDisplayHeader () {
                return true;
            }
        });
        _people.addStyleName("PeopleContainer");

        VerticalPanel placesContainer = new VerticalPanel();
        setWidget(row, 1, placesContainer);
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
        setWidget(row++, 2, gamesContainer);
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
    }

    protected void fillUi (Whirled myWhirled) 
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
                SceneCard card = (SceneCard) sceneIter.next();
                Iterator friendIter = card.friends.iterator();
                while (friendIter.hasNext()) {
                    Object id = friendIter.next();
                    List entry = (List) _peopleAttributes.get(id);
                    if (entry != null) {
                        entry.add(card.sceneType == SceneCard.ROOM ? "Room" : "Game");
                    }
                }
            }
        }

        // ensure that the list has PEOPLE_COLUMNS entries for spacing reasons
        while (people.size() < PEOPLE_COLUMNS) {
            people.add(null);
        }
        _people.setModel(new SimpleDataModel(people), 0);
        _places.populate(myWhirled.places, _peopleAttributes);
        _games.populate(myWhirled.games, _peopleAttributes);
                
        MediaDesc photo = myWhirled.photo == null ? Profile.DEFAULT_PHOTO : myWhirled.photo;
        _pictureBox.add(MediaUtil.createMediaView(photo, 
            // HALF_THUMBNAIL is too small and THUMBNAIL is too big... do something custom
            (int)(MediaDesc.THUMBNAIL_WIDTH * 0.65), (int)(MediaDesc.THUMBNAIL_HEIGHT * 0.65)));

        // show the player's rooms in purchased order by doing an ascending sort on the sceneIds
        Object[] sceneIds = myWhirled.ownedRooms.keySet().toArray();
        Arrays.sort(sceneIds);
        // TODO: if a user has too many rooms, we need to scroll the _roomsBox vertically, 
        // instead of letting it grow indefinitely
        for (int ii = 0; ii < sceneIds.length; ii++) {
            _roomsBox.add(Application.createLink((String)(myWhirled.ownedRooms.get(sceneIds[ii])),
                                                 "world", "s" + sceneIds[ii]));
        }

        if (myWhirled.chats == null || myWhirled.chats.size() == 0) {
            _chatsBox.add(new Label(CWhirled.msgs.noChats()));
        } else {
            // show group chats in group-created order by sorting on group ids
            Object[] chatIds = myWhirled.chats.keySet().toArray();
            Arrays.sort(chatIds);
            for (int ii = 0; ii < chatIds.length; ii++) {
                _chatsBox.add(Application.createLink((String)(myWhirled.chats.get(chatIds[ii])),
                                                     "world", "c" + chatIds[ii]));
            }
        }
    }

    protected static class SceneList extends ScrollPanel
    {
        public SceneList (int sceneType)
        {
            setStyleName("SceneList");
            setAlwaysShowScrollBars(true);
            // why the hell doesn't GWT support only scrolling in one direction?
            DOM.setStyleAttribute(getElement(), "overflowX", "hidden");
        }

        public void populate (List scenes, Map peopleAttributes)
        {
            VerticalPanel sceneContainer = new VerticalPanel();
            sceneContainer.setStyleName("SceneListContainer");
            sceneContainer.setSpacing(0);
            Iterator iter = scenes.iterator();
            while (iter.hasNext()) {
                sceneContainer.add(new SceneWidget((SceneCard) iter.next(), peopleAttributes));
            }
            setWidget(sceneContainer);
        }
    }

    protected static class SceneWidget extends HorizontalPanel
    {
        public SceneWidget (final SceneCard scene, Map peopleAttributes)
        {
            setStyleName("SceneWidget");
            setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

            ClickListener goToScene = new ClickListener() {
                public void onClick (Widget sender) {
                    History.newItem(Application.createLinkToken("world", "s" + scene.sceneId));
                }
            };

            HorizontalPanel logoContainer = new HorizontalPanel();
            logoContainer.setStyleName("LogoContainer");
            logoContainer.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            logoContainer.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            Widget logo = null;
            if (scene.logo != null) {
                logo = MediaUtil.createMediaView(scene.logo, MediaDesc.HALF_THUMBNAIL_SIZE);
            } else if (scene.sceneType == SceneCard.GAME) {
                MediaDesc gameLogo = Item.getDefaultThumbnailMediaFor(Item.GAME);
                logo = MediaUtil.createMediaView(gameLogo, MediaDesc.HALF_THUMBNAIL_SIZE);
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
            String peopleList = "";
            Iterator peopleIter = scene.friends.iterator();
            while (peopleIter.hasNext()) {
                List attrs = (List) peopleAttributes.get(peopleIter.next());
                if (attrs == null) {
                    continue;
                }

                peopleList += "" + attrs.get(0);
                if (peopleIter.hasNext()) {
                    peopleList += ", ";
                } else {
                    peopleList += ".";
                }
            }
            if (peopleList.length() > 45) {
                peopleList = peopleList.substring(0, 42) + "...";
            }
            // Its a little silly that GWT has no way to string together some <span>s
            HTML population = 
                new HTML("<span class='PopulationCount'>" + 
                CWhirled.msgs.population("" + Math.max(scene.population, scene.friends.size())) + 
                "</span><span class='PopulationList>" + peopleList + "</span>");
            text.add(population);
            add(text);
        }
    }

    protected static class PersonWidget extends VerticalPanel
    {
        public PersonWidget (final MemberCard card, Map peopleAttributes)
        {
            setStyleName("PersonWidget");
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

            ClickListener goToFriend = new ClickListener() {
                public void onClick (Widget sender) {
                    WorldClient.displayFlash("memberScene=" + card.name.getMemberId());
                }
            };

            Widget pic = MediaUtil.createMediaView(card.photo, MediaDesc.HALF_THUMBNAIL_SIZE);
            if (pic instanceof Image) {
                ((Image) pic).addClickListener(goToFriend);
            }
            add(pic);
            Label nameLabel = new Label("" + card.name);
            nameLabel.addClickListener(goToFriend);
            nameLabel.setStyleName("NameLabel");
            List attrs = (List) peopleAttributes.get(new Integer(card.name.getMemberId()));
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

    protected VerticalPanel _pictureBox;
    protected VerticalPanel _roomsBox;
    protected VerticalPanel _chatsBox;

    protected HorizontalPanel _errorContainer;

    /** Map of member Ids to a List of attributes for the person.  This list is currently first 
     * the member's name as a string, then second a style name to apply to their name label. */
    protected Map _peopleAttributes;
}
