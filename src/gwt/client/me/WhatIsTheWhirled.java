//
// $Id$

package client.me;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.WhatIsWhirledData;

import client.images.landing.LandingImages;
import client.shell.Application;
import client.shell.Frame;
import client.shell.LogonPanel;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays a summary of what Whirled is and calls to action.
 */
public class WhatIsTheWhirled extends AbsolutePanel
{
    public WhatIsTheWhirled ()
    {
        setStyleName("whatIsTheWhirled");

        final FlowPanel video = new FlowPanel();
        video.setStyleName("Video");
        video.add(MsoyUI.createLabel(CMe.msgs.whatVideo(), "Title"));
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                video.remove(1);
                video.add(WidgetUtil.createFlashContainer(
                              "preview", "/images/landing/slideshow.swf", 360, 260, null));
            }
        };
        video.add(MsoyUI.createActionImage("/images/landing/click_to_play.png",
                                           CMe.msgs.whatClickToStart(), onClick));
        add(video, 40, 225);

        FlowPanel right = new FlowPanel();
        right.setStyleName("Right");
        right.setWidth(RIGHT_COLUMN_WIDTH + "px");
        add(right, RIGHT_COLUMN_X, HEADER_HEIGHT);

        right.add(WidgetUtil.makeShim(10, 34));

        right.add(MsoyUI.createLabel(CMe.msgs.whatFree(), "Label"));
        PushButton signup = new PushButton(_images.signup().createImage(),
                                           Application.createLinkListener(Page.ACCOUNT, "create"));
        signup.getUpHoveringFace().setImage(_images.signup_over().createImage());
        right.add(signup);

        right.add(WidgetUtil.makeShim(10, 35));

        right.add(MsoyUI.createLabel(CMe.msgs.whatTry(), "Label"));
        PushButton play = new PushButton(_images.playgames().createImage(),
                                         Application.createLinkListener(Page.GAMES, ""));
        play.getUpHoveringFace().setImage(_images.playgames_over().createImage());
        right.add(play);

        right.add(WidgetUtil.makeShim(10, 78));

        right.add(MsoyUI.createLabel(CMe.msgs.whatHave(), "LoginTip"));

        PushButton login = new PushButton(_images.login().createImage());
        login.addStyleName("Login");
        login.getUpHoveringFace().setImage(_images.login_over().createImage());
        right.add(new LogonPanel(false, login));
        right.add(login);

        FlowPanel bits = new FlowPanel();
        bits.setStyleName("Bits");
        int year = 1900 + new Date().getYear();
        bits.add(MsoyUI.createHTML(CMe.msgs.whatCopyright(""+year), "inline"));
        bits.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        bits.add(makeLink("http://www.threerings.net", CMe.msgs.whatAbout()));
        bits.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        bits.add(makeLink("http://wiki.whirled.com/Terms_of_Service", CMe.msgs.whatTerms()));
        bits.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        bits.add(makeLink("http://www.threerings.net/about/privacy.html", CMe.msgs.whatPrivacy()));
        bits.add(MsoyUI.createHTML("&nbsp;|&nbsp;", "inline"));
        bits.add(Application.createLink(CMe.msgs.whatHelp(), Page.HELP, ""));
        add(bits, 0, 600);

        CMe.worldsvc.getWhatIsWhirled(new AsyncCallback() {
            public void onSuccess (Object result) {
                showData((WhatIsWhirledData)result);
            }
            public void onFailure (Throwable cause) {
                CMe.log("Failed to load WhatIsWhirledData.", cause);
                // no user feedback, just leave that spot blank
            }
        });
    }

    protected Widget makeLink (String url, String title)
    {
        Anchor anchor = new Anchor(url, title, "_blank");
        anchor.addStyleName("external");
        return anchor;
    }

    protected void showData (WhatIsWhirledData data)
    {
        SmartTable bits = new SmartTable("Stats", 0, 0);
        int[] numbers = { data.players, data.places, data.games };
        for (int ii = 0; ii < numbers.length; ii++) {
            bits.addText(""+numbers[ii], 1, numbers[ii] > 9999 ? "SmallNumber" : "Number");
            bits.addText(LABELS[ii], 1, "Label");
        }
        add(bits, 448, 245);
    }

    protected static final String[] LABELS = {
        CMe.msgs.dataPlayers(), CMe.msgs.dataPlaces(), CMe.msgs.dataGames()
    };

    /** Our screenshot images. */
    protected static LandingImages _images = (LandingImages)GWT.create(LandingImages.class);

    protected static final int WIDTH = 800;
    protected static final int HEIGHT = 630;

    protected static final int HEADER_HEIGHT = 130;
    protected static final int FOOTER_HEIGHT = 28;

    protected static final int RIGHT_COLUMN_X = 553;
    protected static final int RIGHT_COLUMN_WIDTH = 240;
}
