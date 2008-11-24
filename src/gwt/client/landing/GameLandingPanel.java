//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;

/**
 * General landing page featuring games only.
 */
public class GameLandingPanel extends FlowPanel
{
    public GameLandingPanel ()
    {
        setStyleName("gameLandingPanel");
        FlowPanel content = MsoyUI.createFlowPanel("Content");
        add(content);

        FlowPanel main = MsoyUI.createFlowPanel("Main");
        content.add(main);

        main.add(createHeader());
        main.add(createGames());

        FlowPanel footer = MsoyUI.createFlowPanel("Footer");
        content.add(footer);
        footer.add(new LandingCopyright());
    }

    protected Widget createHeader ()
    {
        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");

        // join now
        final Button joinButton = new Button("", Link.createListener(Pages.ACCOUNT, "create"));
        joinButton.setStyleName("JoinButton");
        MsoyUI.addTrackingListener(joinButton, "landingJoinButtonClicked", null);
        header.add(joinButton, 25, 60);

        // logon box
        final FlowPanel logon = new FlowPanel();
        PushButton logonButton = new PushButton(_msgs.landingLogon());
        logonButton.addStyleName("LogonButton");
        LogonPanel lpanel = new LogonPanel(false, logonButton);
        lpanel.setCellSpacing(2); // tighten up!
        logon.add(lpanel);
        logon.add(logonButton);
        header.add(logon, 10, 0);

        // tagline
        final HTML tagline = MsoyUI.createHTML(_msgs.gamesTagline(), null);
        tagline.setStyleName("Tagline");
        header.add(tagline, 15, 140);

        // intro video with click-to-play button
        final SimplePanel video = MsoyUI.createSimplePanel(null, "Video");
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                // controls skin hardcoded in the swf as /images/landing/landing_movie_skin.swf
                video.setWidget(WidgetUtil.createFlashContainer("preview",
                    "/images/landing/landing_movie.swf", 208, 154, null));
            }
        };
        final Image clickToPlayImage = MsoyUI.createInvisiLink(onClick, 208, 154);
        MsoyUI.addTrackingListener(clickToPlayImage, "gameLandingVideoPlayed", null);
        video.setWidget(clickToPlayImage);
        header.add(video, 455, 10);

        return header;
    }

    protected Widget createGames ()
    {
        RoundBox games = new RoundBox(RoundBox.MEDIUM_BLUE);
        games.addStyleName("Games");

        SmartTable gamesTable = new SmartTable("GamesTable", 7, 0);
        gamesTable.setWidget(0, 0, createGame(0));
        gamesTable.setWidget(0, 1, createGame(1));
        gamesTable.setWidget(0, 2, createGame(2));
        gamesTable.setWidget(1, 0, createGame(3));

        FlowPanel center = MsoyUI.createFlowPanel("Center");
        center.add(MsoyUI.createHTML(_msgs.gamesCenterTitle(), "CenterTitle"));
        center.add(MsoyUI.createHTML(_msgs.gamesCenterText(), "CenterText"));
        gamesTable.setWidget(1, 1, center);

        gamesTable.setWidget(1, 2, createGame(4));
        gamesTable.setWidget(2, 0, createGame(5));
        gamesTable.setWidget(2, 1, createGame(6));
        gamesTable.setWidget(2, 2, createGame(7));
        games.add(gamesTable);

        Image allGames = MsoyUI.createActionImage("/images/landing/games_all_games.png",
            Link.createListener(Pages.GAMES, ""));
        allGames.addStyleName("AllGames");
        games.add(allGames);

        return games;
    }

    protected Widget createGame (int gameNum)
    {
        Image game = MsoyUI.createActionImage("/images/landing/games_game_" + gameNum + ".png",
            GAME_NAMES[gameNum], Link.createListener(Pages.GAMES, Args.compose("d",
                GAME_IDS[gameNum])));
        return game;
    }

    // production ids and names of the games displayed on this page
    protected static final int[] GAME_IDS = { 929, 827, 10, 393, 934, 7, 513, 107 };
    protected static final String[] GAME_NAMES = { "Bella Bingo", "Corpse Craft", "Brawler",
        "QBeez", "Fantastic Contraption", "LOLcaptions", "Bomboozle", "Tree House Defense" };

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
