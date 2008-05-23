package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GalaxyData;

import client.games.CGames;
import client.images.landing.LandingImages;
import client.shell.Application;
import client.shell.LogonPanel;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RoundBox;
import client.whirleds.CWhirleds;
import client.whirleds.FeaturedWhirledPanel;

/**
 * Displays a summary of what Whirled is and calls to action.
 */
public class LandingPanel extends AbsolutePanel
{
    public LandingPanel ()
    {
        setStyleName("landingPanel");
        
        // splash with animated characters (left goes over right)
        add(new Image("/images/landing/splash_right.png"), 465, 10);
        final HTML titleAnimation = WidgetUtil.createTransparentFlashContainer(
            "preview", "/images/landing/splash_left.swf", 500, 300, null);
        add(titleAnimation, -23, 10);
        
        // join now
        final Button joinButton = new Button("", Application.createLinkListener(Page.ACCOUNT, "create"));
        joinButton.setStyleName("JoinButton");
        add(joinButton, 475, 0);
        
        // login box
        // TODO LogonPanel is causing scrollbars to appear on the right in IE7 
        final FlowPanel login = new FlowPanel();
        PushButton loginButton = new PushButton(CMe.msgs.landingLogin());
        loginButton.addStyleName("LoginButton");
        login.add(new LogonPanel(true, loginButton)); 
        login.add(loginButton);
        add(login, 590, 0);
             
        // intro video with click-to-play button
        final AbsolutePanel video = new AbsolutePanel();
        video.setStyleName("Video");
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                video.remove(0);
                CMe.app.reportEvent("/me/video");
                // slideshow actual size is 360x260
                video.add(WidgetUtil.createFlashContainer(
                        "preview", "/images/landing/slideshow.swf", 200, 140, null), 38, 9);
            }
        };
        final Image clickToPlayImage = MsoyUI.createActionImage(
                "/images/landing/play_screen.png", CMe.msgs.whatClickToStart(), onClick);
        video.add(clickToPlayImage, 0, 0);
        add(video, 465, 90);
        
        // tagline
        final HTML tagline = new HTML(CMe.msgs.landingTagline());
        tagline.setStyleName("LandingTagline");
        add(tagline, 425, 275);

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
        add(background, 0, 310);
        
        // top games
        final RoundBox games = new RoundBox(RoundBox.DARK_BLUE);
        final TopGamesPanel topGamesPanel = new TopGamesPanel();
        games.add(topGamesPanel);
        add(games, 68, 312);
        CGames.gamesvc.loadTopGamesData(CGames.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                topGamesPanel.setGames((FeaturedGameInfo[])result);
            }
        });
        
        // whirled leaders
        add(new LeadersPanel(), 67, 618);

        // featured whirled panel is beaten into place using css
        _featuredWhirled = new FeaturedWhirledPanel(true);
        add(_featuredWhirled, 290, 618);
        CWhirleds.groupsvc.getGalaxyData(CWhirleds.ident, new MsoyCallback() {
            public void onSuccess (Object result) {
                GalaxyData data = (GalaxyData)result;
                _featuredWhirled.setWhirleds(data.featuredWhirleds);
            }
        });

        // copyright, about, terms & conditions, help
        FlowPanel copyright = new FlowPanel();
        copyright.setStyleName("LandingCopyright");
        int year = 1900 + new Date().getYear();
        copyright.add(MsoyUI.createHTML(CMe.msgs.whatCopyright(""+year), "inline"));
        copyright.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        copyright.add(makeLink("http://www.threerings.net", CMe.msgs.whatAbout()));
        copyright.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        copyright.add(makeLink("http://wiki.whirled.com/Terms_of_Service", CMe.msgs.whatTerms()));
        copyright.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        copyright.add(makeLink("http://www.threerings.net/about/privacy.html", CMe.msgs.whatPrivacy()));
        copyright.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        copyright.add(Application.createLink(CMe.msgs.whatHelp(), Page.HELP, ""));
        add(copyright, 0, 970);
    }

    protected Widget makeLink (String url, String title)
    {
        Anchor anchor = new Anchor(url, title, "_blank");
        anchor.addStyleName("external");
        return anchor;
    }

    protected FeaturedWhirledPanel _featuredWhirled;
    
    /** Our screenshot images. */
    protected static LandingImages _images = (LandingImages)GWT.create(LandingImages.class);
}
