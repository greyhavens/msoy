//
// $Id$

package client.me;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.web.data.MemberCard;

import client.images.next.NextImages;
import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays blurbs about things to do and a player's online friends.
 */
public class WhatsNextPanel extends SmartTable
{
    public WhatsNextPanel (MyWhirledData data)
    {
        super("whatsNext", 0, 0);

        setWidget(0, 0, createPlay(data), 1, "Play");
        setWidget(0, 1, createExplore(data), 1, "Explore");
        if (data.friends == null || data.friends.size() == 0) {
            setWidget(0, 2, createNoFriends(data), 1, "NoFriends");
        } else {
            setWidget(0, 2, createFriends(data), 1, "Friends");
        }
        getFlexCellFormatter().setRowSpan(0, 2, 2);
        setWidget(1, 0, createDecorate(data), 2, "Decorate");
    }

    protected Widget createPlay (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        box.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        Image shot = GAME_SHOTS[Random.nextInt(GAME_SHOTS.length)].createImage();
        ClickListener onClick = Link.createListener(Pages.GAMES, "");
        ClickListener trackListener = MsoyUI.createTrackingListener("mePlayGames", null);
        Image image = MsoyUI.makeActionImage(shot, null, onClick);
        image.addClickListener(trackListener);
        box.add(image);
        box.add(WidgetUtil.makeShim(10, 10));
        PushButton button = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.nextPlay(), onClick);
        button.addClickListener(trackListener);
        box.add(button);
        box.add(WidgetUtil.makeShim(5, 5));
        box.add(MsoyUI.createLabel(_msgs.nextPlayTip(), "tipLabel"));
        return box;
    }

    protected Widget createExplore (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        box.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        Image shot = WHIRLED_SHOTS[Random.nextInt(WHIRLED_SHOTS.length)].createImage();
        ClickListener onClick = Link.createListener(Pages.WHIRLEDS, "");
        ClickListener trackListener = MsoyUI.createTrackingListener("meMakeFriends", null);
        Image image = MsoyUI.makeActionImage(shot, null, onClick);
        image.addClickListener(trackListener);
        box.add(image);
        box.add(WidgetUtil.makeShim(10, 10));
        PushButton button = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.nextExplore(), onClick);
        button.addClickListener(trackListener);
        box.add(button);
        box.add(WidgetUtil.makeShim(5, 5));
        box.add(MsoyUI.createLabel(_msgs.nextExploreTip(), "tipLabel"));
        return box;
    }

    protected Widget createDecorate (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        SmartTable contents = new SmartTable(0, 0);
        ClickListener onClick = Link.createListener(Pages.WORLD, "m" + CMe.getMemberId());
        ClickListener trackListener = MsoyUI.createTrackingListener("meDecorate", null);
        Image image = MsoyUI.makeActionImage(_images.home_shot().createImage(), null, onClick);
        image.addClickListener(trackListener);
        contents.setWidget(0, 0, image, 1, "Screen");
        contents.getFlexCellFormatter().setRowSpan(0, 0, 2);
        PushButton button = MsoyUI.createButton(
            MsoyUI.MEDIUM_THIN, _msgs.nextDecorate(), onClick);
        button.addClickListener(trackListener);
        contents.setWidget(0, 1, button);
        contents.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
        contents.setText(1, 0, _msgs.nextDecorateTip(), 1, "tipLabel");
        box.add(contents);
        return box;
    }

    protected Widget createNoFriends (MyWhirledData data)
    {
        SmartTable friends = new SmartTable(0, 0);
        friends.setHeight("100%");
        friends.setText(0, 0, _msgs.nextFriends(), 1, "Title");
        friends.setText(1, 0, _msgs.nextNoFriends(), 1, "NoFriends");
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
        int size = (data.friends.size() > 6) ?
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
