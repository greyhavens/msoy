//
// $Id$

package client.me;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.person.gwt.MyWhirledData;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;

import client.images.next.NextImages;
import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.ui.TongueBox;
import client.util.Link;

/**
 * Displays blurbs about things to do and a player's online friends.
 */
public class WhatsNextPanel extends TongueBox
{
    public WhatsNextPanel (MyWhirledData data)
    {
        SmartTable content = new SmartTable("whatsNext", 0, 0);
        setContent(content);

        content.setWidget(0, 0, createLeftPanel(), 1, "LeftPanel");
        content.setWidget(0, 1, createNextBadges(data), 1, null);
        if (data.friends == null || data.friends.size() == 0) {
            content.setWidget(0, 2, createNoFriends(data), 1, "NoFriends");
        } else {
            content.setWidget(0, 2, createFriends(data), 1, "Friends");
        }
    }

    protected Widget createLeftPanel ()
    {
        FlowPanel panel = new FlowPanel();
        panel.add(MsoyUI.createImage("/images/me/passport_icon_large.png", "PassportIcon"));
        panel.add(MsoyUI.createLabel(_msgs.nextPassportTip(), "PassportTip"));
        return panel;
    }

    protected Widget createNextBadges (MyWhirledData data)
    {
        SmartTable badges = new SmartTable("NextBadges", 0, 0);
        badges.setText(0, 0, _msgs.nextBadgesTitle(), 3, "NextTitle");

        for (int row = 0; row < 2; row++) {
            if (data.badges.size() > row * 2) {
                badges.setWidget(row + 1, 0, new BadgeDisplay(data.badges.get(row * 2)));
            } else {
                badges.setWidget(row + 1, 0, MsoyUI.createFlowPanel("Spacer"));
            }
            badges.setWidget(row + 1, 1, MsoyUI.createSimplePanel(MsoyUI.createImage("/images/me/passport_box_divider.png", null),
                "Divider"));
            if (data.badges.size() > row * 2 + 1) {
                badges.setWidget(row + 1, 2, new BadgeDisplay(data.badges.get(row * 2 + 1)));
            } else {
                badges.setWidget(row + 1, 2, MsoyUI.createFlowPanel("Spacer"));
            }
        }

        badges.setWidget(
            3, 0, Link.create(_msgs.nextBadgesSeeAll(), Pages.ME, "passport"), 3,  "SeeAllLink");
        return badges;
    }

    protected Widget createNoFriends (MyWhirledData data)
    {
        SmartTable friends = new SmartTable(0, 0);
        friends.setHeight("100%");
        friends.setText(0, 0, _msgs.nextFriends(), 1, "Title");
        friends.setText(1, 0, _msgs.nextNoFriends(), 1, "NoFriendsMsg");
        Widget imageLink = Link.createImage(
            "/images/me/invite_friends.png", _msgs.nextInviteTip(), Pages.PEOPLE, "invites");
        ((SourcesClickEvents)imageLink).addClickListener(
            MsoyUI.createTrackingListener("meInviteFriends", null));
        friends.setWidget(2, 0, imageLink);
        friends.setText(3, 0, _msgs.nextOr(), 1, "Or");
        friends.setText(4, 0, _msgs.nextFind(), 1, "Title");

        HorizontalPanel sctrls = new HorizontalPanel();
        sctrls.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        final TextBox search = MsoyUI.createTextBox("", -1, -1);
        search.setWidth("150px");
        sctrls.add(search);
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                String query = search.getText().trim();
                if (query.length() > 0) {
                    Link.go(Pages.PEOPLE, Args.compose("search", "0", query));
                }
            }
        };
        search.addKeyboardListener(new EnterClickAdapter(onClick));
        sctrls.add(WidgetUtil.makeShim(5, 5));
        sctrls.add(new Button("Search", onClick));

        friends.setWidget(5, 0, sctrls);
        friends.setText(6, 0, _msgs.nextFindTip(), 1, "FindTip");
        return friends;
    }

    protected Widget createFriends (MyWhirledData data)
    {
        FlowPanel friends = new FlowPanel();
        friends.add(MsoyUI.createLabel(_msgs.nextFriends(), "Title"));
        friends.add(MsoyUI.createLabel(_msgs.nextFriendClick(), "ClickTip"));

        // if we have few friends, show larger photo images
        int size = (data.friends.size() > 5) ?
            MediaDesc.QUARTER_THUMBNAIL_SIZE : MediaDesc.HALF_THUMBNAIL_SIZE;

        // group our friends by location (in rooms or games)
        Map<Integer, FlowPanel> games = new HashMap<Integer, FlowPanel>();
        Map<Integer, FlowPanel> rooms = new HashMap<Integer, FlowPanel>();
        for (MemberCard card : data.friends) {
            if (card.status instanceof MemberCard.InScene) {
                int sceneId = ((MemberCard.InScene)card.status).sceneId;
                FlowPanel room = getPlacePanel(
                    rooms, sceneId, ((MemberCard.InScene)card.status).sceneName);
                ClickListener onClick = Link.createListener(Pages.WORLD, "s"+sceneId);
                Widget member = makeMemberWidget(card, size, onClick);
                member.addStyleName("MemberRoom");
                room.add(member);

            } else if (card.status instanceof MemberCard.InAVRGame) {
                int sceneId = ((MemberCard.InAVRGame)card.status).sceneId;
                FlowPanel room = getPlacePanel(
                    rooms, sceneId, ((MemberCard.InAVRGame)card.status).gameName);
                ClickListener onClick = Link.createListener(Pages.WORLD, "s"+sceneId);
                Widget member = makeMemberWidget(card, size, onClick);
                member.addStyleName("MemberGame");
                room.add(member);

            } else if (card.status instanceof MemberCard.InGame) {
                int gameId = ((MemberCard.InGame)card.status).gameId;
                FlowPanel game = getPlacePanel(
                    games, gameId, ((MemberCard.InGame)card.status).gameName);
                ClickListener onClick = Link.createListener(
                    Pages.WORLD, Args.compose("game", "l", ""+gameId));
                Widget member = makeMemberWidget(card, size, onClick);
                member.addStyleName("MemberGame");
                game.add(member);
            }
        }

        // now add the rooms and games to our scrolling contents (rooms first)
        for (FlowPanel panel : rooms.values()) {
            friends.add(panel);
        }
        for (FlowPanel panel : games.values()) {
            friends.add(panel);
        }

        ScrollPanel scroller = new ScrollPanel(friends);
        scroller.addStyleName("FriendsScroller");
        return scroller;
    }

    protected FlowPanel getPlacePanel (
        Map<Integer, FlowPanel> places, int placeId, String placeName)
    {
        FlowPanel place = places.get(placeId);
        if (place == null) {
            places.put(placeId, place = new FlowPanel());
            place.add(MsoyUI.createLabel(placeName, "PlaceName"));
        }
        return place;
    }

    protected Widget makeMemberWidget (MemberCard card, int size, ClickListener onClick)
    {
        SmartTable member = new SmartTable("Member", 0, 0);
        member.setWidget(0, 0, new ThumbBox(card.photo, size, onClick), 1, "Photo");
        member.setWidget(0, 1, MsoyUI.createActionLabel(card.name.toString(), null, onClick),
                         1, "Name");
        return member;
    }

    protected static final NextImages _images = GWT.create(NextImages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);

    protected static final AbstractImagePrototype[] GAME_SHOTS = {
        _images.astro_shot(), _images.brawler_shot(), _images.dict_shot(),
        _images.drift_shot(), _images.lol_shot()
    };

    protected static final AbstractImagePrototype[] WHIRLED_SHOTS = {
        _images.brave_shot(), _images.kawaii_shot(), _images.nap_shot(),
        _images.pirate_shot(), _images.rave_shot()
    };
}
