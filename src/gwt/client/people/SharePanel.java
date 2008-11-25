//
// $Id$

package client.people;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Panel for sharing the Whirled with the ones you love and anyone else.
 */
public class SharePanel extends VerticalPanel
{
    public SharePanel ()
    {
        setStyleName("share");
        setSpacing(10);

        // introduction to sharing
        SmartTable intro = new SmartTable();
        intro.setStyleName("MainHeader");
        intro.setWidget(0, 0, new Image("/images/people/share_header.png"));
        intro.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareIntro(), "MainHeaderText"));
        add(intro);

        // link to whirled
        SmartTable linkToWhirled = new SmartTable();
        linkToWhirled.setStyleName("SubHeader");
        linkToWhirled.setWidget(0, 0, new Image("/images/people/link_to_whirled.png"));
        TextBox embedText = new TextBox();
        embedText.setText(getAffiliateLandingUrl());
        embedText.setMaxLength(100);
        embedText.setWidth("100%");
        FlowPanel shareText = new FlowPanel();
        shareText.add(MsoyUI.createHTML(_msgs.shareLinkToWhirled(), null));
        shareText.add(embedText);
        linkToWhirled.setWidget(0, 1, shareText, 1, "SubHeaderText");
        RoundBox linkToWhirledBox = new RoundBox(RoundBox.DARK_BLUE);
        linkToWhirledBox.add(linkToWhirled);
        add(linkToWhirledBox);

        // share content
        SmartTable share = new SmartTable();
        share.setStyleName("SubHeader");
        share.setWidget(0, 0, new Image("/images/people/share.png"));
        share.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareLinkToContent(), "SubHeaderText"));
        RoundBox shareBox = new RoundBox(RoundBox.DARK_BLUE);
        shareBox.add(share);
        add(shareBox);

        // invite friends
        SmartTable email = new SmartTable();
        email.setStyleName("SubHeader");
        email.setWidget(0, 0, new Image("/images/people/send_email.png"));
        email.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareSendEmail(), "SubHeaderText"));
        InvitePanel invitePanel = new InvitePanel(false, false, email);
        invitePanel.setSpacing(0);
        add(invitePanel);
    }
    
    public static String getAffiliateLandingUrl ()
    {
        return DeploymentConfig.serverURL + "welcome/" + CShell.creds.getMemberId();
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
