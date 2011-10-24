//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.FBLogonPanel;
import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.ui.RegisterPanel;
import client.util.Link;

/**
 * Our main landing page.
 */
public class LandingPanel extends SmartTable
{
    public LandingPanel (boolean playNow)
    {
        super("landing", 0, 20);

        // create a UI explaining briefly what Whirled is
        FlowPanel explain = MsoyUI.createFlowPanel("Explain");
        explain.add(MsoyUI.createLabel(_msgs.landingIntro(), "Title"));
        explain.add(MsoyUI.createLabel(_msgs.landingIntroSub(), "Subtitle"));
        final SimplePanel video = new SimplePanel();
        video.setStyleName("Video");
        video.setWidget(MsoyUI.createActionImage("/images/landing/play_screen.png",
                                                 _msgs.landingClickToStart(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                video.setWidget(WidgetUtil.createFlashContainer(
                    "preview", "/images/landing/landing_movie.swf", 208, 154, null));
            }
        }));
        explain.add(video);

        FlowPanel rightbits = MsoyUI.createFlowPanel("Rightbits");
        if (playNow) {
            // create our play now UI
            FlowPanel playNowPlaceholder = MsoyUI.createFlowPanel("playNowPlaceholder");
            playNowPlaceholder.add(MsoyUI.createLabel(_msgs.landingHopRightIn(), "Subtitle"));
            SmartTable align = new SmartTable(0, 5);
            align.setWidth("100%");
            align.setWidget(0, 0, MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
                _msgs.landingPlayNow(), Link.createHandler(Pages.WORLD, "places")));
            align.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_RIGHT);
            playNowPlaceholder.add(align);
            rightbits.add(playNowPlaceholder);

        } else {
            // create our registration UI
            FlowPanel registerPlaceholder = MsoyUI.createFlowPanel("registerPlaceholder");
            RegisterPanel register = new RegisterPanel() {
                protected void addHeader (boolean complete) {
                    if (complete) {
                        add(MsoyUI.createLabel(_msgs.landingRegistered(), "Title"));
                    } else {
                        add(MsoyUI.createLabel(_msgs.landingRegister(), "Title"));
                        add(MsoyUI.createLabel(_msgs.landingRegisterSub(), "Subtitle"));
                    }
                }
            };
            registerPlaceholder.add(register);
            rightbits.add(registerPlaceholder);
        }

        // create a logon UI
        rightbits.add(WidgetUtil.makeShim(15, 15));
        rightbits.add(MsoyUI.createLabel(_msgs.landingLogon(), "Subtitle"));
        TextBox logemail = MsoyUI.createTextBox("", MemberName.MAX_EMAIL_LENGTH, -1);
        PasswordTextBox logpass = new PasswordTextBox();
        ButtonBase doLogon = MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.landingLogGo(), null);
        LogonPanel.addLogonBehavior(logemail, logpass, doLogon, null);
        SmartTable logon = new SmartTable("register", 0, 5); // hack!
        logon.setText(0, 0, _msgs.landingLogEmail(), 1, "Right");
        logon.setWidget(0, 1, logemail);
        logon.setText(1, 0, _msgs.landingLogPass(), 1, "Right");
        logon.setWidget(1, 1, logpass);
        logon.setWidget(2, 1, LogonPanel.newForgotPassword(logemail.getText().trim()));
        logon.setWidget(3, 1, doLogon);
        logon.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        for (int row = 0; row < logon.getRowCount(); row++) {
            logon.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }
        rightbits.add(logon);

        FlowPanel connect = MsoyUI.createFlowPanel("Connect");
        connect.add(MsoyUI.createLabel(_msgs.landingConnect(), "Subtitle"));
        connect.add(new FBLogonPanel());
        rightbits.add(connect);

        // wrap all that up in two columns with header and background and whatnot
        setWidget(0, 0, explain);
        getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_LEFT);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, rightbits);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        setWidget(1, 0, LandingCopyright.addFinePrint(new FlowPanel()), 2);
        getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
    }

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
