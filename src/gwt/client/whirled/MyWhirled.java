//
// $Id$

package client.whirled;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.Whirled;

import client.util.FlashClients;
import client.util.MediaUtil;

public class MyWhirled extends FlexTable
{
    public MyWhirled ()
    {
        buildUi();

        CWhirled.membersvc.getMyWhirled(CWhirled.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                Whirled data = (Whirled) result;
                _friendLocations = new HashMap();
                fillFriendLocations(data.rooms);
                fillFriendLocations(data.games);
                _rooms.setModel(new SimpleDataModel(data.rooms), 0);
                _games.setModel(new SimpleDataModel(data.games), 0);
                _people.setModel(new SimpleDataModel(data.people), 0);
            }
            public void onFailure (Throwable caught) {
                _errorContainer.add(new Label(CWhirled.serverError(caught)));
            }
        });
    }

    protected void buildUi ()
    {
        int row = 0;

        setWidget(row++, 0, _errorContainer = new HorizontalPanel());

        setWidget(row++, 0, _rooms = new PagedGrid(ROOMS_ROWS, ROOMS_COLUMS) {
            protected Widget createWidget (Object item) {
                return new SceneWidget((SceneCard) item);
            }
            protected String getEmptyMessage () {
                return CWhirled.msgs.noRooms();
            }
            protected String getHeaderText (int start, int limit, int total) {
                return CWhirled.msgs.headerRooms();
            }
            protected boolean alwaysDisplayHeader () {
                return true;
            }
        });

        setWidget(row++, 0, _games = new PagedGrid(GAMES_ROWS, GAMES_COLUMS) {
            protected Widget createWidget (Object item) {
                return new SceneWidget((SceneCard) item);
            }
            protected String getEmptyMessage () {
                return CWhirled.msgs.noGames();
            }
            protected String getHeaderText (int start, int limit, int total) {
                return CWhirled.msgs.headerGames();
            }
            protected boolean alwaysDisplayHeader () {
                return true;
            }
        });

        setWidget(row++, 0, _people = new PagedGrid(PEOPLE_ROWS, PEOPLE_COLUMS) {
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

    protected static class SceneWidget extends HorizontalPanel
    {
        public SceneWidget (SceneCard scene)
        {
            add(MediaUtil.createMediaView(scene.logo, MediaDesc.HALF_THUMBNAIL_SIZE));
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

    protected static final int ROOMS_ROWS = 1;
    protected static final int ROOMS_COLUMS = 2;
    protected static final int GAMES_ROWS = 1;
    protected static final int GAMES_COLUMS = 2;
    protected static final int PEOPLE_ROWS = 1;
    protected static final int PEOPLE_COLUMS = 2;

    protected PagedGrid _rooms;
    protected PagedGrid _games;
    protected PagedGrid _people;

    protected HorizontalPanel _errorContainer;

    protected HashMap _friendLocations;
}
