//
// $Id$

package client.people;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Panel for sharing the Whirled with the ones you love and anyone else.
 */
public class SharePanel extends VerticalPanel
{
    public SharePanel ()
    {
        this(getAffiliateLandingUrl(Pages.LANDING));
    }

    public SharePanel (String gameId, String defaultMessage, String gameToken, String type)
    {
        this(createGameURL(gameId, gameToken, type));
        _invitePanel.setMessage(decodeMsg(defaultMessage));
    }

    public SharePanel (String url)
    {
        setStyleName("share");
        setSpacing(10);

        // introduction to sharing
        SmartTable intro = new SmartTable();
        intro.setStyleName("MainHeader");
        intro.setWidget(0, 0, new Image("/images/people/share_header.png"));
        intro.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareIntro(), "MainHeaderText"));
        add(intro);

        // invite friends
        SmartTable email = new SmartTable();
        email.setStyleName("SubHeader");
        email.setWidget(0, 0, new Image("/images/people/send_email.png"));
        email.setWidget(0, 1, MsoyUI.createHTML(_msgs.shareSendEmail(), "SubHeaderText"));
        _invitePanel = new InvitePanel(false, email);
        _invitePanel.setSpacing(0);
        add(_invitePanel);
    }

    public static String getAffiliateLandingUrl (Pages page, Object ...args)
    {
        String path = DeploymentConfig.serverURL + "welcome/" + CShell.creds.getMemberId();
        if (page != Pages.LANDING) {
            path += "/" + Pages.makeToken(page, Args.compose(args));
        }
        return path;
    }

    protected static String decodeMsg (String src)
    {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        while (index < src.length()) {
            char c = src.charAt(index);
            if (c == '\\') {
                if (index == src.length() - 1 || src.charAt(index + 1) == '\\') {
                    sb.append('\\');
                    index++;
                } else if (src.charAt(index + 1) == '-') {
                    sb.append('_');
                    index++;
                }
            } else {
                sb.append(c);
            }
            index++;
        }
        return sb.toString();
    }

    protected static String createGameURL (String gameId, String gameToken, String type)
    {
        boolean isAVRG = type.startsWith("avrg");
        return DeploymentConfig.serverURL + "welcome/" + CShell.creds.getMemberId() + "/world-" +
            (isAVRG ? "s" + type.substring(4) + "_world_" : "") + "game_t_" + gameId + "_" +
            CShell.creds.getMemberId() + "_" + gameToken;
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);

    protected final InvitePanel _invitePanel;
}
