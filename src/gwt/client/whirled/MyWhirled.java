//
// $Id$

package client.whirled;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.DOM;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
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

import client.util.FlashClients;
import client.util.MediaUtil;

import client.shell.Application;

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
        header = new HorizontalPanel();
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.setStyleName("SectionHeaderContainer");
        title = new Label(CWhirled.msgs.headerChats());
        title.setStyleName("SectionHeader");
        header.add(title);
        mePanel.add(header);
        mePanel.add(_chatsBox = new VerticalPanel());
        _roomsBox.setStyleName("RoomsBox");
        _chatsBox.addStyleName("borderedBox");
        
        setWidget(row++, 1, _errorContainer = new HorizontalPanel());

        getFlexCellFormatter().setColSpan(row, 1, 2);
        setWidget(row++, 1, _people = new PagedGrid(PEOPLE_ROWS, PEOPLE_COLUMS) {
            protected Widget createWidget (Object item) {
                return new PersonWidget((MemberCard) item, _friendLocations);
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
        _people.setWidth("539px");

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
        _friendLocations = new HashMap();
        fillFriendLocations(myWhirled.places);
        fillFriendLocations(myWhirled.games);
        _people.setModel(new SimpleDataModel(myWhirled.people), 0);
                
        MediaDesc photo = myWhirled.photo == null ? Profile.DEFAULT_PHOTO : myWhirled.photo;
        _pictureBox.add(MediaUtil.createMediaView(photo, 
            // HALF_THUMBNAIL is too small and THUMBNAIL is too big... do something custom
            (int)(MediaDesc.THUMBNAIL_WIDTH * 0.65), (int)(MediaDesc.THUMBNAIL_HEIGHT * 0.65)));

        // show the player's rooms in purchased order by doing an ascending sort on the sceneIds
        Object[] sceneIds = myWhirled.ownedRooms.keySet().toArray();
        Arrays.sort(sceneIds);
        for (int ii = 0; ii < sceneIds.length; ii++) {
            _roomsBox.add(Application.createLink((String)(myWhirled.ownedRooms.get(sceneIds[ii])),
                                                 "world", "s" + sceneIds[ii]));
        }
    }

    protected void fillFriendLocations (List scenes) {
        Iterator sceneIter = scenes.iterator();
        while (sceneIter.hasNext()) {
            SceneCard card = (SceneCard) sceneIter.next();
            String text = card.sceneType == SceneCard.ROOM ? 
                CWhirled.msgs.inRoom("" + card.name) : CWhirled.msgs.inGame("" + card.name);
            Iterator friendIter = card.friends.iterator();
            while (friendIter.hasNext()) {
                _friendLocations.put(friendIter.next(), text);
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
    }

    protected static class SceneWidget extends HorizontalPanel
    {
        public SceneWidget (SceneCard scene)
        {
            if (scene.logo != null) {
                add(MediaUtil.createMediaView(scene.logo, MediaDesc.HALF_THUMBNAIL_SIZE));
            } else if (scene.sceneType == SceneCard.GAME) {
                MediaDesc logo = Item.getDefaultThumbnailMediaFor(Item.GAME);
                add(MediaUtil.createMediaView(logo, MediaDesc.HALF_THUMBNAIL_SIZE));
            }
            Label nameLabel = new Label("" + scene.name);
            nameLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    // TODO
                }
            });
            add(nameLabel);
        }
    }

    protected static class PersonWidget extends HorizontalPanel
    {
        public PersonWidget (final MemberCard card, Map friendLocations)
        {
            add(MediaUtil.createMediaView(card.photo, MediaDesc.HALF_THUMBNAIL_SIZE));

            VerticalPanel text = new VerticalPanel();
            Label nameLabel = new Label("" + card.name);
            nameLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    FlashClients.goMemberScene(card.name.getMemberId());
                }
            });
            text.add(nameLabel);
            text.add(new Label("" + friendLocations.get(new Integer(card.name.getMemberId()))));
            add(text);
        }
    }

    protected static final int PEOPLE_ROWS = 1;
    protected static final int PEOPLE_COLUMS = 2;

    protected PagedGrid _people;
    protected SceneList _places;
    protected SceneList _games;

    protected VerticalPanel _pictureBox;
    protected VerticalPanel _roomsBox;
    protected VerticalPanel _chatsBox;

    protected HorizontalPanel _errorContainer;

    protected HashMap _friendLocations;
}
