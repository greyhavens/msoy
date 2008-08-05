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

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.landing.gwt.LandingData;
import com.threerings.msoy.landing.gwt.LandingService;
import com.threerings.msoy.landing.gwt.LandingServiceAsync;

import client.images.landing.LandingImages;
import client.shell.LogonPanel;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.whirleds.FeaturedWhirledPanel;

/**
 * Displays a summary of what Whirled is, featuring games, avatars and whirleds.
 * Spans the entire width of the page, with an active content area 800 pixels wide and centered.
 */
public class LandingPanel extends SimplePanel
{
    public LandingPanel ()
    {
        // LandingPanel contains LandingBackground contains LandingContent
        setStyleName("LandingPanel");
        SimplePanel headerBackground = new SimplePanel();
        headerBackground.setStyleName("LandingBackground");
        AbsolutePanel content = new AbsolutePanel();
        content.setStyleName("LandingContent");
        headerBackground.setWidget(content);
        this.setWidget(headerBackground);

        // splash with animated characters (left goes over right)
        final HTML titleAnimation = WidgetUtil.createTransparentFlashContainer(
            "preview", "/images/landing/splash_left.swf", 500, 300, null);
        content.add(titleAnimation, -23, 10);

        // join now
        final Button joinButton =
            new Button("", Link.createListener(Pages.ACCOUNT, "create"));
        joinButton.setStyleName("JoinButton");
        joinButton.addClickListener(
            MsoyUI.createTrackingListener("landingJoinButtonClicked", null));
        content.add(joinButton, 475, 0);

        // login box
        final FlowPanel login = new FlowPanel();
        PushButton loginButton = new PushButton(_msgs.landingLogin());
        loginButton.addStyleName("LoginButton");
        login.add(new LogonPanel(true, loginButton, true));
        login.add(loginButton);
        content.add(login, 590, 0);

        // intro video with click-to-play button
        final AbsolutePanel video = new AbsolutePanel();
        video.setStyleName("Video");
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                video.remove(0);
                // slideshow actual size is 360x260
                video.add(WidgetUtil.createFlashContainer(
                        "preview", "/images/landing/slideshow.swf", 200, 140, null), 38, 9);
            }
        };
        final Image clickToPlayImage = MsoyUI.createActionImage(
                "/images/landing/play_screen.png", _msgs.landingClickToStart(), onClick);
        clickToPlayImage.addClickListener(
            MsoyUI.createTrackingListener("landingVideoPlayed", null));
        video.add(clickToPlayImage, 0, 0);
        content.add(video, 465, 90);

        // tagline
        final HTML tagline = new HTML(_msgs.landingTagline());
        tagline.setStyleName("LandingTagline");
        content.add(tagline, 425, 275);

        // background for the rest of the page
        final FlowPanel background = new FlowPanel();
        background.setStyleName("Background");
        final FlowPanel leftBorder = new FlowPanel();
        leftBorder.setStyleName("LeftBorder");
        background.add(leftBorder);
        final FlowPanel center = new FlowPanel();
        center.setStyleName("Center");
        background.add(center);
        final FlowPanel rightBorder = new FlowPanel();
        rightBorder.setStyleName("RightBorder");
        background.add(rightBorder);
        content.add(background, 0, 310);

        // top games
        final RoundBox games = new RoundBox(RoundBox.DARK_BLUE);
        final TopGamesPanel topGamesPanel = new TopGamesPanel();
        games.add(topGamesPanel);
        content.add(games, 68, 312);

        // featured avatar
        content.add(_avatarPanel = new AvatarPanel(), 67, 618);

        // featured whirled panel is beaten into place using css
        _featuredWhirled = new FeaturedWhirledPanel(true, true);
        content.add(_featuredWhirled, 290, 618);

        // copyright, about, terms & conditions, help
        content.add(new LandingCopyright(), 48, 970);

        // collect the data for this page
        _landingsvc.getLandingData(new MsoyCallback<LandingData>() {
            public void onSuccess (LandingData data) {
                topGamesPanel.setGames(data.topGames);
                _featuredWhirled.setWhirleds(data.featuredWhirleds);
                _avatarPanel.setAvatars(data.topAvatars);
            }
        });
    }

    protected FeaturedWhirledPanel _featuredWhirled;
    protected AvatarPanel _avatarPanel;

    protected static final LandingImages _images = GWT.create(LandingImages.class);
    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
    protected static final LandingServiceAsync _landingsvc = (LandingServiceAsync)
        ServiceUtil.bind(GWT.create(LandingService.class), LandingService.ENTRY_POINT);
}
