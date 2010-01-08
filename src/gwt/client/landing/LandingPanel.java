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
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.LogonPanel;
import client.ui.MsoyUI;
import client.ui.RegisterPanel;
import client.util.Link;

/**
 * Our main landing page.
 */
public class LandingPanel extends SmartTable
{
    public LandingPanel ()
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

        // create our registration UI and a logon UI
        FlowPanel rightbits = MsoyUI.createFlowPanel("Rightbits");
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
        logon.setWidget(2, 1, doLogon);
        logon.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        for (int row = 0; row < logon.getRowCount(); row++) {
            logon.getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_MIDDLE);
        }
        rightbits.add(logon);

        FlowPanel connect = MsoyUI.createFlowPanel("Connect");
        connect.add(MsoyUI.createLabel(_msgs.landingConnect(), "Subtitle"));
        connect.add(Link.createImage("/images/account/fbconnect.png", null,
                                     Pages.ACCOUNT, "logon"));
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

    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
