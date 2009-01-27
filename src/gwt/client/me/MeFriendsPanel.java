//
// $Id: WhatsNextPanel.java 12616 2008-10-17 21:40:55Z mdb $

package client.me;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.person.gwt.MyWhirledData;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a list of friends online and/or a way to find friends online, for display on the Me
 * page.
 */
public class MeFriendsPanel extends FlowPanel
{
    public MeFriendsPanel (MyWhirledData data)
    {
        setStyleName("meFriendsPanel");
        add(MsoyUI.createLabel(_msgs.nextFriends(), "Title"));
        if (data.friends == null || data.friends.size() == 0) {
            addNoFriends(data);
        } else {
            add(MsoyUI.createLabel(_msgs.nextFriendClick(), "TitleSub"));
            addPeople(data.friends);
        }

        add(MsoyUI.createHTML(_msgs.nextGreeters(), "GreeterTitleSub"));
        if (data.greeters != null && data.greeters.size() > 0) {
            addPeople(data.greeters);
        }
        add(new Image("/images/me/me_friends_footer.png"));
    }

    /**
     * Add widgets displayed when player has no friends.
     */
    protected void addNoFriends (MyWhirledData data)
    {
        add(MsoyUI.createLabel(_msgs.nextNoFriends(), "TitleSub"));

        Widget imageLink = Link.createImage(
            "/images/me/me_invite_friends.png", _msgs.nextInviteTip(), Pages.PEOPLE, "invites");
        imageLink.addStyleName("Invite");
        MsoyUI.addTrackingListener(((SourcesClickEvents)imageLink), "meInviteFriends", null);
        add(imageLink);

        add(MsoyUI.createLabel(_msgs.nextOr(), "Or"));
        add(MsoyUI.createLabel(_msgs.nextFind(), "SearchTitle"));

        FloatPanel searchControls = new FloatPanel("Search");
        final TextBox search = MsoyUI.createTextBox("", -1, -1);
        search.setWidth("120px");
        searchControls.add(search);
        add(searchControls);
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                String query = search.getText().trim();
                if (query.length() > 0) {
                    Link.go(Pages.PEOPLE, Args.compose("search", "0", query));
                }
            }
        };
        search.addKeyboardListener(new EnterClickAdapter(onClick));
        searchControls.add(new Button("Go", onClick));

        add(searchControls);
        add(MsoyUI.createLabel(_msgs.nextFindTip(), null));
    }

    /**
     * Add widgets displayed when player has friends, who may or may not be online.
     */
    protected void addPeople (List<MemberCard> people)
    {
        // contents will scroll after a long time
        FlowPanel friendsList = MsoyUI.createFlowPanel("FriendsList");
        int size = MediaDesc.HALF_THUMBNAIL_SIZE;

        // group our friends by location (in rooms or games)
        Map<Integer, FlowPanel> games = new HashMap<Integer, FlowPanel>();
        Map<Integer, FlowPanel> rooms = new HashMap<Integer, FlowPanel>();
        for (MemberCard card : people) {
            if (card.status instanceof MemberCard.InScene) {
                int sceneId = ((MemberCard.InScene)card.status).sceneId;
                FlowPanel room = getPlacePanel(
                    rooms, sceneId,
                    ((MemberCard.InScene)card.status).sceneName, false);
                Widget member = makeMemberWidget(card, size, Pages.WORLD, "s"+sceneId);
                member.addStyleName("MemberRoom");
                room.add(member);

            } else if (card.status instanceof MemberCard.InAVRGame) {
                int sceneId = ((MemberCard.InAVRGame)card.status).sceneId;
                FlowPanel room = getPlacePanel(
                    rooms, sceneId,
                    ((MemberCard.InAVRGame)card.status).gameName, true);
                Widget member = makeMemberWidget(card, size, Pages.WORLD, "s"+sceneId);
                member.addStyleName("MemberGame");
                room.add(member);

            } else if (card.status instanceof MemberCard.InGame) {
                int gameId = ((MemberCard.InGame)card.status).gameId;
                FlowPanel game = getPlacePanel(
                    games, gameId,
                    ((MemberCard.InGame)card.status).gameName, true);
                Widget member =
                    makeMemberWidget(card, size, Pages.WORLD, Args.compose("game", "l", ""+gameId));
                member.addStyleName("MemberGame");
                game.add(member);
            }
        }

        // now add the rooms and games to our scrolling contents (rooms first)
        for (FlowPanel panel : rooms.values()) {
            friendsList.add(panel);
        }
        for (FlowPanel panel : games.values()) {
            friendsList.add(panel);
        }
        add(friendsList);
    }

    protected FlowPanel getPlacePanel (
        Map<Integer, FlowPanel> places, int placeId,
        String placeName, boolean isGame)
    {
        FlowPanel place = places.get(placeId);
        if (place == null) {
            places.put(placeId, place = new FlowPanel());
            place.add(MsoyUI.createLabel(isGame ? _msgs.friendPlaying(placeName)
                : _msgs.friendIn(placeName), "PlaceName"));
        }
        return place;
    }

    protected Widget makeMemberWidget (MemberCard card, int size, Pages page, String args)
    {
        SmartTable member = new SmartTable("Member", 0, 0);
        member.setWidget(0, 0, new ThumbBox(card.photo, size, page, args), 1, "Photo");
        member.setWidget(0, 1, Link.create(card.name.toString(), page, args), 1, "Name");
        return member;
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
