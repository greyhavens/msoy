//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

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

    protected void showData (WhatIsWhirledData data)
    {
        FlexTable bits = new FlexTable();
        bits.setCellPadding(0);
        bits.setCellSpacing(0);
        bits.setStyleName("Stats");
        int[] numbers = { data.players, data.places, data.games };
        for (int ii = 0; ii < numbers.length; ii++) {
            int row = 2*ii;
            bits.setText(row, 0, ""+numbers[ii]);
            bits.getFlexCellFormatter().setStyleName(row, 0, "Number");
            bits.setText(row+1, 0, LABELS[ii]);
            bits.getFlexCellFormatter().setStyleName(row+1, 0, "Label");
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
