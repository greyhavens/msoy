//
// $Id$

package client.whirled;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WhatIsWhirledData;

import client.shell.Application;
import client.shell.LogonPanel;
import client.shell.Page;
import client.shell.Frame;
import client.util.MsoyUI;

/**
 * Displays a summary of what Whirled is and calls to action.
 */
public class WhatIsTheWhirled extends AbsolutePanel
{
    public WhatIsTheWhirled ()
    {
        setStyleName("whatIsTheWhirled");

        add(MsoyUI.createActionImage("/images/landing/signup.jpg", new ClickListener() {
            public void onClick (Widget widget) {
                Application.go(Page.ACCOUNT, "create");
            }
        }), 540, 152);

        add(MsoyUI.createActionImage("/images/landing/playgames.jpg", new ClickListener() {
            public void onClick (Widget widget) {
                Application.go(Page.GAME, "");
            }
        }), 540, 303);

        Button logon = new Button("");
        logon.addStyleName("Logon");
        add(logon, 674, 551);

        add(new LogonPanel(false, logon), 555, 440);

        CWhirled.worldsvc.getWhatIsWhirled(new AsyncCallback() {
            public void onSuccess (Object result) {
                showData((WhatIsWhirledData)result);
            }
            public void onFailure (Throwable cause) {
                CWhirled.log("Failed to load WhatIsWhirledData.", cause);
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
        add(bits, 440, 300);
    }

    protected static final String[] LABELS = {
        CWhirled.msgs.dataPlayers(), CWhirled.msgs.dataPlaces(), CWhirled.msgs.dataGames()
    };
}
