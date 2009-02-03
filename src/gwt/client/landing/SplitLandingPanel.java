//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page split into games on the left and rooms on the right.
 */
public class SplitLandingPanel extends FlowPanel
{
    public SplitLandingPanel ()
    {
        // shares style with the roomsLandingPanel
        setStyleName("splitLandingPanel");
        addStyleName("BlueLandingPage");

        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        // big header image with login and video
        content.add(createHeader());

        content.add(MsoyUI.createHTML(_msgs.splitIntro(), "IntroBlurb"));

        FloatPanel columns = new FloatPanel("Columns");
        columns.add(createGamesPanel());
        columns.add(createRoomsPanel());
        content.add(columns);

        content.add(MsoyUI.createHTML(_msgs.splitCreate(), "CreateBlurb"));

        // PushButton joinButton = MsoyUI.createButton(MsoyUI.LONG_THICK, _msgs.splitJoin(),
        // Link.createListener(Pages.ACCOUNT, "create"));
        // joinButton.addStyleName("JoinNowButton");
        // content.add(joinButton);

        // footer stretches full width, contains copyright info
        add(MsoyUI.createSimplePanel(new LandingCopyright(), "Footer"));
    }

    protected Widget createHeader ()
    {
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");

        // join now
        header.add(MsoyUI.createActionImage("/images/landing/join_now_button.png",
            Link.createListener(Pages.ACCOUNT, "create")), 50, 60);

        // logon box
        final FlowPanel logon = new FlowPanel();
        PushButton logonButton = new PushButton(_msgs.landingLogon());
        logonButton.addStyleName("LogonButton");
        logon.add(new LogonPanel(LogonPanel.Mode.LANDING, logonButton));
        logon.add(logonButton);
        header.add(logon, 40, 0);

        // tagline
        // final HTML tagline = MsoyUI.createHTML(_msgs.gamesTagline(), null);
        // tagline.setStyleName("Tagline");
        // header.add(tagline, 35, 140);

        // intro video with click-to-play button
        final SimplePanel video = MsoyUI.createSimplePanel(null, "Video");
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                // controls skin hardcoded in the swf as /images/landing/landing_movie_skin.swf
                video.setWidget(WidgetUtil.createFlashContainer("preview",
                    "/images/landing/landing_movie.swf", 208, 154, null));
            }
        };
        onClick = MsoyUI.makeTrackingListener("landingGames VideoPlayed", null, onClick);
        final Image clickToPlayImage = MsoyUI.createInvisiLink(onClick, 208, 154);
        video.setWidget(clickToPlayImage);
        header.add(video, 569, 41);

        return header;
    }

    protected Widget createGamesPanel ()
    {
        AbsolutePanel panel = MsoyUI.createAbsolutePanel("Games");
        panel.add(MsoyUI.createLabel(_msgs.splitGamesTitle(), "Title"));

        // pick a random game to feature at the top
        panel.add(_featuredGame = MsoyUI.createAbsolutePanel("Featured"));
        int randomIndex = (int)(Math.random() * GAME_IDS.length);
        selectGame(randomIndex);

        // list 4 other games below it
        FloatPanel otherItems = new FloatPanel("OtherItems");
        for (int i = 0; i < 4; i++) {
            final int otherIndex = (randomIndex + i) % GAME_IDS.length;
            FocusPanel otherItem = new FocusPanel();
            otherItem.setStyleName("OtherItem");
            otherItem.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    selectGame(otherIndex);
                }
            });
            otherItem.add(MsoyUI.createLabel(GAME_NAMES[otherIndex], null));
            otherItems.add(otherItem);

//            otherItems.add(MsoyUI.createActionLabel(GAME_NAMES[otherIndex], "OtherItem",
//                new ClickListener() {
//                    public void onClick (Widget sender) {
//                        selectGame(otherIndex);
//                    }
//                }));
        }
        panel.add(otherItems);

        panel.add(MsoyUI.createHTML(_msgs.splitGamesBlurb(), "Blurb"));
        return panel;
    }

    protected Widget createRoomsPanel ()
    {
        AbsolutePanel panel = MsoyUI.createAbsolutePanel("Rooms");
        panel.add(MsoyUI.createLabel(_msgs.splitRoomsTitle(), "Title"));

        // pick a random room to feature at the top
        panel.add(_featuredRoom = MsoyUI.createAbsolutePanel("Featured"));
        int randomIndex = (int)(Math.random() * ROOM_IDS.length);
        selectRoom(randomIndex);

        // list 4 other rooms below it
        FloatPanel otherItems = new FloatPanel("OtherItems");
        for (int i = 0; i < 4; i++) {
            final int otherIndex = (randomIndex + 1 + i) % ROOM_IDS.length;
            FocusPanel otherItem = new FocusPanel();
            otherItem.setStyleName("OtherItem");
            otherItem.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    selectRoom(otherIndex);
                }
            });
            otherItem.add(MsoyUI.createLabel(ROOM_NAMES[otherIndex], null));
            otherItems.add(otherItem);
        }
        panel.add(otherItems);

        panel.add(MsoyUI.createHTML(_msgs.splitRoomsBlurb(), "Blurb"));
        return panel;
    }

    protected void selectGame (int index)
    {
        _featuredGame.clear();
        _featuredGame.add(MsoyUI.createActionImage(
            "/images/landing/split_game_" + index + ".png", GAME_NAMES[index],
            Link.createListener(Pages.GAMES, Args.compose("d", GAME_IDS[index]))), 10, 0);
        _featuredGame.add(MsoyUI.createLabel(GAME_NAMES[index], "Name"), 220, 0);
        _featuredGame.add(MsoyUI.createLabel(GAME_DESCRIPTIONS[index], "Description"), 220, 30);
        _featuredGame.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.splitGamesPlay(),
            Link.createListener(Pages.GAMES, Args.compose("d", GAME_IDS[index]))), 220, 104);
    }

    protected void selectRoom (int index)
    {
        _featuredRoom.clear();
        _featuredRoom.add(MsoyUI.createActionImage(
            "/images/landing/split_room_" + index + ".png", ROOM_NAMES[index],
            Link.createListener(Pages.WORLD, "s" + ROOM_IDS[index])), 10, 0);
        _featuredRoom.add(MsoyUI.createLabel(ROOM_NAMES[index], "Name"), 220, 0);
        _featuredRoom.add(MsoyUI.createLabel(ROOM_DESCRIPTIONS[index], "Description"), 220, 30);
        _featuredRoom.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.splitRoomsVisit(),
            Link.createListener(Pages.WORLD, "s" + ROOM_IDS[index])), 220, 104);
    }

    protected AbsolutePanel _featuredGame;
    protected AbsolutePanel _featuredRoom;

    // production ids and names of the games displayed on this page
    protected static final int[] GAME_IDS = { 929, 827, 10, 393, 934, 7, 513 };
    protected static final String[] GAME_NAMES = { "Bella Bingo", "Corpse Craft", "Brawler",
        "QBeez", "Fantastic Contraption", "LOLcaptions", "Bomboozle" };
    protected static final String[] GAME_DESCRIPTIONS = {
        "Bingo for fashionistas - match accessories to win.  Play with up to 99 players!",
        "Build an army of reanimated corpses to destroy your foes in this puzzle-action hybrid!",
        "A side scrolling crawl-n-brawl game involving swords, gnolls, and cuteness!",
        "Just get em together and then click, click, click! The puzzle game with personality!",
        "Build contraptions to cross mind-bending levels in this brain-stretching puzzle game.",
        "Think up captions for Flickr pictures and then vote for the best one.  Multiplayer!",
        "Draw lines and detonate bombs to destroy the cutesy blobs in this relaxing puzzler!" };

    // production ids and names of the rooms displayed on this page
    protected static final int[] ROOM_IDS = { 107217, 119439, 89294, 94819, 99355, 116771,
        130834, 101315 };
    protected static final String[] ROOM_NAMES = { "Little Red's Forest", "Whirled Train Ride",
        "Little Monsters", "Aquatica Whirled", "Exploring", "The Emerald", "Paper Whirled",
        "The Lionheart" };
    protected static final String[] ROOM_DESCRIPTIONS = {
        "By Pareia.  A Choose Your Own Adventure storybook forest - watch out for the wolf!",
        "By Omer.  Hang out on a train and watch the countryside roll by outside the windows.",
        "Mr Dot has combined photography, sculpture, and painting in this fascinating space.",
        "Experience life under the sea and design your own aquarium.  By Pronouncedyou!",
        "Let your dreams take flight as you explore the cosmos from Wingedmaquis' bedroom.",
        "By Booradley.  Join the crew of a pirate ship and explore the ocean with Mer-tofu!",
        "Olsn's fantastic paper landscape changes depending on the time of day.",
        "Serena's Airship sails the seven winds in search of adventure - come aboard!" };

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
